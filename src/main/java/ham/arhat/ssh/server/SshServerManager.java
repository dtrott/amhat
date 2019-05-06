package ham.arhat.ssh.server;

import ham.arhat.config.facade.ConfigurationFactory;
import ham.arhat.config.facade.SshServerConfig;
import ham.arhat.config.support.TargetRoute;
import ham.arhat.control.ControlConnections;
import ham.arhat.hash.client.HashClientManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Slf4j
public class SshServerManager implements SessionListener {
    private final ConfigurationFactory configFactory;
    private final CommandFactory commandFactory;
    private final HashClientManager hashClientManager;
    private final ControlConnections controlConnections;
    private volatile SshServer sshServer;

    public void initialize() {
        final SshServerConfig sshServerConfig = configFactory.getSshServerConfig();
        final int listenPort = sshServerConfig.getListenPort();

        if (listenPort <= 0)
            return;

        try {
            this.sshServer = startSshServer(
                    listenPort,
                    sshServerConfig.getHostKeyPairs(),
                    sshServerConfig.getPublicKeyAuthenticator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SshServer startSshServer(
            int listenPort,
            KeyPairProvider keyPairProvider,
            PublickeyAuthenticator publickeyAuthenticator) throws IOException {

        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(listenPort);

        sshServer.addSessionListener(this);
        this.commandFactory.setSshFactories(sshServer);
        sshServer.setPublickeyAuthenticator(publickeyAuthenticator);

        sshServer.setKeyPairProvider(keyPairProvider);
        sshServer.start();
        return sshServer;
    }

    private void sessionAuthenticated(Session session) {
        final String authUsername = session.getUsername();
        log.info("Authenticated: " + authUsername);

        final TargetRoute targetRoute = configFactory.getControlConfig().getTargetRoute(authUsername);

        if (targetRoute.isValid()) {
            hashClientManager.connect(session, targetRoute);
        } else {
            // TODO handle this case.
        }
    }

    @Override
    public void sessionEvent(Session session, Event event) {
        if (event == Event.Authenticated) {
            sessionAuthenticated(session);
        }
    }

    @Override
    public void sessionClosed(Session session) {
        controlConnections.sshSessionClosed(session);
    }
}
