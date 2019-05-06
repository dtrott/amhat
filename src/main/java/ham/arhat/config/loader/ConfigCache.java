package ham.arhat.config.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import ham.arhat.config.facade.ConfigurationFactory;
import ham.arhat.config.facade.ControlConfig;
import ham.arhat.config.facade.RemoteConfig;
import ham.arhat.config.facade.SshClientConfig;
import ham.arhat.config.facade.SshServerConfig;
import ham.arhat.config.facade.TransportConfig;
import ham.arhat.config.file.Config;
import ham.arhat.config.file.ConfigRemoteServer;
import ham.arhat.config.file.ConfigSshUser;
import ham.arhat.config.file.ConfigTransport;
import ham.arhat.config.impl.RemoteImpl;
import ham.arhat.config.impl.SshClientImpl;
import ham.arhat.config.impl.SshServerImpl;
import ham.arhat.config.impl.TransportImpl;
import ham.arhat.config.support.TargetRoute;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.common.util.security.SecurityUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConfigCache implements ConfigurationFactory {

    private final ObjectMapper objectMapper;
    @Getter
    private volatile Config config;

    private final RemoteImpl remote = new RemoteImpl();
    private final SshClientImpl sshClient = new SshClientImpl();
    private final SshServerImpl sshServer = new SshServerImpl();
    private final TransportImpl transport = new TransportImpl();

    private final KeyLoader keyLoader = new KeyLoader(sshServer, transport, sshClient);

    @Override
    public ControlConfig getControlConfig() {
        return this::getTargetRoute;
    }

    @Override
    public RemoteConfig getRemoteConfig() {
        return remote;
    }

    @Override
    public SshClientConfig getSshClientConfig() {
        return sshClient;
    }

    @Override
    public SshServerConfig getSshServerConfig() {
        return sshServer;
    }

    @Override
    public TransportConfig getTransportConfig() {
        return transport;
    }

    public void initialize() {
        // Force bouncy castle to load..
        SecurityUtils.isBouncyCastleRegistered();

        try {
            this.config = objectMapper.readValue(new File("config.json"), Config.class);

            updateMissingConfig(config);

            sshServer.update(config.getSshServer());
            this.transport.update(config.getTransport());

            keyLoader.loadKeys(config);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateMissingConfig(Config config) {
        final ConfigTransport configTransport = config.getTransport();
        // If signing key is missing use the first ssh client key as the signing key.
        if (configTransport.getSigningKey() == null)
            configTransport.setSigningKey(config.getSshClient().getPrivateKeys().get(0));

        if (configTransport.getInboundAuthorizedKeys() == null)
            configTransport.setInboundAuthorizedKeys(config.getSshServer().getAuthorizedKeys());

        if (configTransport.getOutboundAuthorizedKeys() == null)
            configTransport.setOutboundAuthorizedKeys(configTransport.getInboundAuthorizedKeys());
    }

    private TargetRoute getTargetRoute(String username) {
        final Map<String, ConfigSshUser> users = config.getSshServer().getUsers();
        final Map<String, ConfigRemoteServer> remoteServers = config.getRemoteServers();

        final ConfigSshUser user = users.get(username);
        if (user != null) {
            final ConfigRemoteServer server = remoteServers.get(user.getRemoteServer());

            if (server != null)
                return newTargetNode(server, user);
        }

        return new TargetRoute(false, null, null, null);
    }

    private TargetRoute newTargetNode(ConfigRemoteServer remote, ConfigSshUser user) {
        final InetSocketAddress remoteNode = new InetSocketAddress(
                remote.getHostname(), remote.getPort());

        return new TargetRoute(true, remote.getAuthenticationUser(), remoteNode, user.getEndPoint());
    }

}
