package ham.arhat.config.facade;

public interface ConfigurationFactory {
    ControlConfig getControlConfig();

    RemoteConfig getRemoteConfig();

    SshClientConfig getSshClientConfig();

    SshServerConfig getSshServerConfig();

    TransportConfig getTransportConfig();
}
