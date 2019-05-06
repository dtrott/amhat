package ham.arhat.control;

import ham.arhat.config.support.TargetRoute;
import ham.arhat.hash.shared.ShutdownMode;
import ham.arhat.message.CloseMessage;
import ham.arhat.message.ConnectMessage;
import ham.arhat.message.SshCommandMessage;
import ham.arhat.ssh.server.CommandCapture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.session.Session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
class ControlConnection {
    static final String ATTRIB_CONTROL_CONNECTION = "controlConnection";

    private final TargetRoute targetRoute;
    private final Session sshSession;
    private final AtomicInteger nextCommandId = new AtomicInteger();
    private final ConcurrentHashMap<Integer, ControlCommand> commands = new ConcurrentHashMap<>();
    private volatile IoSession hashSession;
    private volatile ShutdownMode shutdownMode;

    void initializeSshSession() {
        sshSession.getIoSession().setAttribute(ATTRIB_CONTROL_CONNECTION, this);
    }

    static ControlConnection get(Session session) {
        return (ControlConnection) session.getIoSession().getAttribute(ATTRIB_CONTROL_CONNECTION);
    }

    void initializeHashSession(IoSession session) {
        session.setAttribute(ATTRIB_CONTROL_CONNECTION, this);
        this.hashSession = session;
    }

    ControlCommand newControlCommand(CommandCapture capture) {
        final ControlCommand command = new ControlCommand(this, nextCommandId.incrementAndGet(), capture);
        this.commands.put(command.getCommandId(), command);
        return command;
    }

    void writeMessage(Object message) {
        writeMessage(message, 0);
    }

    void writeMessage(Object message, int seconds) {
        final IoSession hashSession = this.hashSession;
        if (hashSession == null) {
            return;
        }

        final WriteFuture writeFuture;
        synchronized (hashSession) {
            writeFuture = hashSession.write(message);
        }

        if (seconds > 0)
            writeFuture.awaitUninterruptibly(seconds, TimeUnit.SECONDS);
    }

    void commandComplete(int commandId) {
        commands.remove(commandId);
    }

    void sshSessionClosed() {
        shutdown(ShutdownMode.APPLICATION);
    }

    void hashSessionOpened() {
        writeMessage(new ConnectMessage(targetRoute));
    }

    void hashMessageReceived(Object obj) {
        if (!(obj instanceof SshCommandMessage))
            return;

        final SshCommandMessage message = (SshCommandMessage) obj;

        final ControlCommand command = commands.get(message.getCommandId());
        if (command != null)
            command.hashMessageReceived(message);
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

    private void shutdown(ShutdownMode mode) {
        if (setShutdownMode(mode)) {
            for (ControlCommand command : commands.values()) {
                command.shutdown(mode);
            }

            finalizeShutdown();
        }
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

        }
    }
}
