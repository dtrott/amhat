package ham.arhat.control;

import ham.arhat.hash.shared.ShutdownMode;
import ham.arhat.message.CommandExitedMessage;
import ham.arhat.message.DataChannel;
import ham.arhat.message.DestroyCommandMessage;
import ham.arhat.message.SendDataMessage;
import ham.arhat.message.SignalWindowChangeMessage;
import ham.arhat.message.SshCommandMessage;
import ham.arhat.message.StartCommandMessage;
import ham.arhat.ssh.server.CommandCapture;
import ham.arhat.ssh.server.CommandType;
import lombok.Getter;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Getter
public class ControlCommand implements SignalListener {

    private final ControlConnection connection;
    private final int commandId;
    private final CommandType commandType;
    private final String command;
    private final ChannelSession session;
    private final InputDataReceiver in;
    private final IoOutputStream out;
    private final IoOutputStream err;
    private final ExitCallback callback;
    private volatile Environment environment;
    private volatile boolean shutdownSignaled;

    public ControlCommand(ControlConnection connection, int commandId, CommandCapture capture) {
        this.connection = connection;
        this.commandId = commandId;
        this.commandType = capture.getCommandType();
        this.command = capture.getCommand();
        this.session = capture.getSession();
        this.in = new InputDataReceiver();
        this.out = capture.getOut();
        this.err = capture.getErr();
        this.callback = capture.getCallback();
    }

    public void start(Environment env) {
        this.environment = env;
        env.addSignalListener(this);
        session.setDataReceiver(this.in);
        connection.writeMessage(new StartCommandMessage(commandType, commandId, command, env.getEnv()),0);
    }

    void shutdown(ShutdownMode mode) {
        synchronized (this) {
            if (shutdownSignaled)
                return;

            shutdownSignaled = true;
        }
        connection.commandComplete(commandId);
    }

    public void destroy() {
        connection.writeMessage(new DestroyCommandMessage(commandId));
    }

    @Override
    public void signal(Signal signal) {
        if (signal == Signal.WINCH) {
            final Map<String, String> env = environment.getEnv();

            final int columns = Integer.parseInt(env.get(Environment.ENV_COLUMNS));
            final int lines = Integer.parseInt(env.get(Environment.ENV_LINES));

            connection.writeMessage(
                    new SignalWindowChangeMessage(this.commandId, columns, lines),1);
        }
    }

    void hashMessageReceived(SshCommandMessage message) {
        switch (message.getType()) {
            case SSH_SEND_DATA:
                dataReceived((SendDataMessage) message);
                break;
            case SSH_COMMAND_EXITED:
                commandExited((CommandExitedMessage) message);
                break;
        }
    }

    private void dataReceived(SendDataMessage message) {
        try {
            final ByteArrayBuffer buffer = new ByteArrayBuffer(message.getData());
            final IoOutputStream stream = getOutputStream(message);

            stream.writePacket(buffer).verify();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void commandExited(CommandExitedMessage message) {
        callback.onExit(message.getExitValue(), message.getExitMessage());
    }

    private IoOutputStream getOutputStream(SendDataMessage message) {
        switch (message.getDataChannel()) {
            case OUT:
                return out;
            case ERR:
                return err;
            default:
                throw new IllegalStateException("Unable to match channel");
        }
    }

    private class InputDataReceiver implements ChannelDataReceiver {
        @Override
        public int data(ChannelSession channel, byte[] buf, int start, int len) throws IOException {
            if (len > 0)
                connection.writeMessage(
                        new SendDataMessage(commandId, DataChannel.IN, Arrays.copyOfRange(buf, start, start + len)), 30);

            return len;
        }

        @Override
        public void close() throws IOException {
            shutdown(ShutdownMode.CLEAN);
        }
    }
}
