package ham.arhat.config.file;

import lombok.Data;

import java.util.Map;

@Data
public class Config {
    private ConfigSshClient sshClient = new ConfigSshClient();
    private ConfigSshServer sshServer = new ConfigSshServer();
    private Map<String, ConfigRemoteServer> remoteServers;
    private ConfigTransport transport = new ConfigTransport();
}
