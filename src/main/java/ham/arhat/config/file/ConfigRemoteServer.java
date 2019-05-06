package ham.arhat.config.file;

import lombok.Data;

@Data
public class ConfigRemoteServer {
    private String authenticationUser = "";
    private String hostname;
    private int port;
}
