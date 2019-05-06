package ham.arhat.core;

import lombok.Data;

@Data
public class ConnectionRequest {
    private final String username;
    private final String remoteServerName;
}
