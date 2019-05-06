package ham.arhat.remote;

import ham.arhat.config.facade.SshClientConfig;
import ham.arhat.hash.shared.ShutdownMode;
import ham.arhat.message.CloseMessage;
import ham.arhat.message.CommandExitedMessage;
import ham.arhat.message.ConnectMessage;
import ham.arhat.message.SshCommandMessage;
import ham.arhat.message.StartCommandMessage;
import ham.arhat.message.TransportMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class RemoteConnection {
    public static final String ATTRIB_REMOTE_CONNECTION = "remoteConnection";

    private final SshClientConfig sshClientConfig;
    private final IoSession hashSession;
    private final SshClient sshClient;
    private volatile ClientSession clientSession;
    private final ConcurrentHashMap<Integer, RemoteCommand<?>> commands = new ConcurrentHashMap<>();
    private volatile ShutdownMode shutdownMode;

    void hashMessageReceived(Object obj) {
        if (!(obj instanceof TransportMessage)) {
            final Class cls = obj == null ? null : obj.getClass();
            final String name = cls == null ? null : cls.getName();

            log.warn("Received unexpected message, type: " + name);
            return;
        }

        TransportMessage transportMessage = (TransportMessage) obj;

        transportMessageReceived(transportMessage);
    }

    void hashSessionIdle(IdleStatus status) {
    }

    void hashInputClosed() {
        shutdown(ShutdownMode.CLEAN);
    }

    void hashSessionClosed() {
        shutdown(ShutdownMode.TRANSPORT);
    }

    void hashExceptionCaught(Throwable cause) {
        log.error("Shutting transport down due to exception", cause);
        shutdown(ShutdownMode.TRANSPORT);
    }

    private void transportMessageReceived(TransportMessage transportMessage) {
        switch (transportMessage.getType()) {
            case SSH_CONNECT:
                sshConnect((ConnectMessage) transportMessage);
                break;
            case SSH_START_COMMAND:
                startCommand((StartCommandMessage) transportMessage);
                break;
            default:
                if (transportMessage instanceof SshCommandMessage) {
                    SshCommandMessage message = (SshCommandMessage) transportMessage;
                    final RemoteCommand<?> command = commands.get(message.getCommandId());

                    if (command != null)
                        command.hashMessageReceived(message);
                } else {
                    log.warn("Unable to process message: " + transportMessage.getType());
                }
                break;
        }
    }

    private void sshConnect(ConnectMessage connectMessage) {

        final String username = connectMessage.getRemoteUsername();
        final String hostname = connectMessage.getRemoteHostname();
        final int port = connectMessage.getRemotePort();

        final ClientSession clientSession;

        try {
            clientSession = sshClient.connect(username, hostname, port)
                    .verify(1000)
                    .getSession();

        } catch (IOException e) {
            log.error("Unable to connect to host: " + hostname, e);
            shutdown(ShutdownMode.APPLICATION);
            return;
        }

        sshClientConfig.getPublicKeyIdentities().forEach(clientSession::addPublicKeyIdentity);

        try {
            clientSession.auth().verify(1000);
            this.clientSession = clientSession;
        } catch (IOException e) {
            log.error("Authentication Failed: " + hostname, e);
            shutdown(ShutdownMode.APPLICATION);
            clientSession.close(true);
        }
    }

    private void startCommand(StartCommandMessage startCommand) {
        final RemoteCommand<?> command;

        synchronized (this) {
            if (isShuttingDown())
                return;

            command = createCommand(startCommand);
            if (command == null) {
                log.error("Unknown command type: " + startCommand.getType());
                return;
            }

            this.commands.put(command.getCommandId(), command);
        }
        command.start(clientSession, startCommand.getEnvironment());
    }

    private RemoteCommand<?> createCommand(StartCommandMessage startCommand) {
        switch (startCommand.getCommandType()) {
            case SHELL:
                return new RemoteCommandShell(this, startCommand);
            case EXEC:
                return new RemoteCommandExec(this, startCommand);
            case SFTP:
                return new RemoteCommandSftp(this, startCommand);
            default:
                return null;
        }
    }

    WriteFuture writeMessage(Object message) {
        synchronized (hashSession) {
            return hashSession.write(message);
        }
    }

    void commandExited(int commandId, Integer exitStatus, String exitMessage) {
        final ShutdownMode mode = this.shutdownMode;

        if (mode == null || mode == ShutdownMode.CLEAN)
            writeMessage(new CommandExitedMessage(commandId, exitStatus, exitMessage));

        synchronized (this) {
            commands.remove(commandId);
            finalizeShutdown();
        }
    }

    private void shutdown(ShutdownMode mode) {
        if (setShutdownMode(mode)) {
            for (RemoteCommand command : commands.values()) {
                command.shutdown(mode);
            }

            finalizeShutdown();
        }
    }

    private synchronized boolean isShuttingDown() {
        return shutdownMode != null;
    }

    private synchronized boolean setShutdownMode(ShutdownMode mode) {
        if (this.shutdownMode == null) {
            this.shutdownMode = mode;
            return true;
        }

        return false;
    }

    private synchronized void finalizeShutdown() {
        if (commands.isEmpty() && this.shutdownMode != null) {

            if (hashSession != null) {
                writeMessage(CloseMessage.class);
            }

            if (clientSession != null) {
                clientSession.close(false);
                clientSession = null;
            }
        }
    }
}
