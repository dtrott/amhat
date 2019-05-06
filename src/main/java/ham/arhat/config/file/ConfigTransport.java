package ham.arhat.config.file;

import lombok.Data;

@Data
public class ConfigTransport {

    private int connectTimeout;

    private int listenPort;

    private ConfigPrivateKey signingKey;

    private String inboundAuthorizedKeys;

    private String outboundAuthorizedKeys;
}
