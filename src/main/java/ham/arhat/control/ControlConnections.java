package ham.arhat.control;

import ham.arhat.config.support.TargetRoute;
import ham.arhat.ssh.server.CommandCapture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.session.Session;

import javax.inject.Singleton;
import java.util.function.Consumer;

import static ham.arhat.control.ControlConnection.ATTRIB_CONTROL_CONNECTION;

@Singleton
public class ControlConnections extends IoHandlerAdapter {

    public void initializeHashSession(TargetRoute targetRoute, Session sshSession, IoSession session) {
        final ControlConnection controlConnection = new ControlConnection(targetRoute, sshSession);
        controlConnection.initializeSshSession();
        controlConnection.initializeHashSession(session);
    }

    public ControlCommand newControlCommand(CommandCapture capture) {
        final ControlConnection controlConnection = ControlConnection.get(capture.getSession().getSession());
        return controlConnection == null ?
                null :
                controlConnection.newControlCommand(capture);
    }

    public void sshSessionClosed(Session sshSession) {
        final ControlConnection controlConnection = ControlConnection.get(sshSession);

        if (controlConnection != null)
            controlConnection.sshSessionClosed();
    }

    // Callbacks from outbound hash connection.

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        fireHashEvent(session, ControlConnection::hashSessionOpened);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        fireHashEvent(session, x -> x.hashMessageReceived(message));
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        fireHashEvent(session, x -> x.hashSessionIdle(status));
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {
        fireHashEvent(session, ControlConnection::hashInputClosed);

    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        fireHashEvent(session, ControlConnection::hashSessionClosed);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        fireHashEvent(session, x -> x.hashExceptionCaught(cause));
    }

    private void fireHashEvent(IoSession session, Consumer<ControlConnection> callback) {
        final ControlConnection controlConnection = (ControlConnection) session.getAttribute(
                ATTRIB_CONTROL_CONNECTION);

        if (controlConnection != null)
            callback.accept(controlConnection);
    }
}
