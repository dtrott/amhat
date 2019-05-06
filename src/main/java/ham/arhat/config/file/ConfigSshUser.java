package ham.arhat.config.file;

import ham.arhat.config.support.EndPoint;
import lombok.Data;

@Data
public class ConfigSshUser {
    private String remoteServer;
    private EndPoint endPoint;
}
