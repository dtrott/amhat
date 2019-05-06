package ham.arhat.remote;

import ham.arhat.config.facade.ConfigurationFactory;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RemoteConnections extends IoHandlerAdapter {

    private final ConfigurationFactory configFactory;
    private volatile SshClient sshClient;

    public void initialize() {
        final SshClient outboundSshClient = SshClient.setUpDefaultClient();
        outboundSshClient.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
        outboundSshClient.setServerKeyVerifier(new KnownHostsServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE, Paths.get("/Users/dtrott/known")));
        outboundSshClient.start();

        this.sshClient = outboundSshClient;
    }

    // Callbacks from outbound hash connection.

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        final RemoteConnection remoteConnection = new RemoteConnection(
                configFactory.getSshClientConfig(), session, this.sshClient);
        session.setAttribute(RemoteConnection.ATTRIB_REMOTE_CONNECTION, remoteConnection);
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
        fireHashEvent(session, x -> x.hashInputClosed());
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        fireHashEvent(session, x -> x.hashSessionClosed());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        fireHashEvent(session, x -> x.hashExceptionCaught(cause));
    }

    private void fireHashEvent(IoSession session, Consumer<RemoteConnection> callback) {
        final RemoteConnection remoteConnection = (RemoteConnection) session.getAttribute(
                RemoteConnection.ATTRIB_REMOTE_CONNECTION);

        if (remoteConnection != null)
            callback.accept(remoteConnection);
    }
}
