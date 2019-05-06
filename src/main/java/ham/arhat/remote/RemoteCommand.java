package ham.arhat.remote;

import ham.arhat.hash.shared.ShutdownMode;
import ham.arhat.message.DataChannel;
import ham.arhat.message.SendDataMessage;
import ham.arhat.message.SignalWindowChangeMessage;
import ham.arhat.message.SshCommandMessage;
import ham.arhat.message.StartCommandMessage;
import ham.arhat.ssh.server.CommandType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelListener;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ham.arhat.message.DataChannel.IN;
import static org.apache.sshd.client.channel.ClientChannel.Streaming.Async;

@Getter
@Slf4j
public abstract class RemoteCommand<C extends ClientChannel> {

    private final RemoteConnection connection;
    private final int commandId;
    private final CommandType commandType;
    private final String command;

    protected volatile C channel;
    private volatile ShutdownMode shutdownMode;
    private boolean closeChannelSignaled;

    // Count for process exit + 2 streams (out & err) - can't do anything with in stream.
    protected final AtomicInteger closeCounter = new AtomicInteger(3);

    public RemoteCommand(RemoteConnection connection, StartCommandMessage startCommand) {
        this.connection = connection;
        this.commandId = startCommand.getCommandId();
        this.commandType = startCommand.getCommandType();
        this.command = startCommand.getCommandLine();
    }

    public void start(ClientSession session, Map<String, String> environment) {
        final C channel;
        try {
            channel = openChannel(session, this.command);
        } catch (IOException e) {
            connection.commandExited(commandId, -1, e.getMessage());
            return;
        }

        channel.setStreaming(Async);
        configurePty(environment, channel);

        channel.addChannelListener(new ChannelListener() {
            @Override
            public void channelClosed(Channel channel_, Throwable reason) {
                handleCommandExit();
            }
        });

        this.channel = channel;

        try {
            channel.open().verify(1000);
        } catch (IOException e) {
            this.channel = null;
            channel.close(true);
            connection.commandExited(commandId, -2, e.getMessage());
            return;
        }

        forwardStream(channel.getAsyncOut(), DataChannel.OUT);
        forwardStream(channel.getAsyncErr(), DataChannel.ERR);
    }

    private void forwardStream(IoInputStream asyncStream, final DataChannel dataChannel) {
        asyncStream.read(new ByteArrayBuffer())
                .addListener(new SshFutureListener<IoReadFuture>() {
                    @Override
                    public void operationComplete(IoReadFuture future) {
                        try {
                            future.verify(5L, TimeUnit.SECONDS);

                            Buffer buffer = future.getBuffer();
                            if (buffer.available() > 0) {
                                final byte[] bytes = new byte[buffer.available()];
                                buffer.getRawBytes(bytes, 0, bytes.length);
                                connection.writeMessage(new SendDataMessage(commandId, dataChannel, bytes))
                                        .addListener(x -> nextRead(buffer));
                            } else {
                                nextRead(buffer);
                            }
                        } catch (IOException e) {
                            handleCommandExit();

                            if (!channel.isClosing())
                                closeChannel(true);

                        }
                    }

                    private void nextRead(Buffer buffer) {
                        buffer.compact();
                        asyncStream.read(buffer).addListener(this);
                    }
                });
    }

    void hashMessageReceived(SshCommandMessage message) {
        switch (message.getType()) {
            case SSH_SEND_DATA:
                hashDataReceived((SendDataMessage) message);
                break;
            case SSH_SIGNAL_WINDOW_CHANGE:
                final SignalWindowChangeMessage winChange = (SignalWindowChangeMessage) message;
                sendWindowChange(winChange.getColumns(), winChange.getLines());
                break;
            case SSH_DESTROY_COMMAND:
                shutdown(ShutdownMode.CLEAN);
                break;
        }
    }

    private void hashDataReceived(SendDataMessage message) {
        if (isCloseChannelSignaled() || shutdownMode != null) {
            log.warn("Received data after process exited.");
            return;
        }

        if (message.getDataChannel() != IN) {
            log.error("Received data on invalid chanel: " + message.getDataChannel());
            return;
        }

        try {
            synchronized (channel) {
                channel.getAsyncIn().writePacket(new ByteArrayBuffer(message.getData())).verify();
            }
        } catch (IOException e) {
            if (shutdownMode == null) {
                log.error("Failed to write to remote client", e);
                closeChannel(false);
            }
        }
    }

    private synchronized boolean isCloseChannelSignaled() {
        return closeChannelSignaled;
    }

    private synchronized boolean setCloseChannelSignaled() {
        boolean oldValue = closeChannelSignaled;
        closeChannelSignaled = true;
        return oldValue;
    }

    private void handleCommandExit() {
        final int remainingHandles = closeCounter.decrementAndGet();
        if (remainingHandles == 0) {
            closeChannel(true);
        }
    }

    private void closeChannel(boolean immediately) {
        if (setCloseChannelSignaled())
            return;

        channel.close(immediately).addListener(x -> {
            connection.commandExited(commandId, channel.getExitStatus(), channel.getExitSignal());
        });
    }

    void shutdown(ShutdownMode mode) {
        this.shutdownMode = mode;
        closeChannel(false);
    }

    abstract protected C openChannel(ClientSession session, String command) throws IOException;

    abstract protected void configurePty(final Map<String, String> environment, C channel);

    abstract protected void sendWindowChange(int columns, int lines);
}
