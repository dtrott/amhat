package ham.arhat.config.impl;

import ham.arhat.config.facade.TransportConfig;
import ham.arhat.config.file.ConfigTransport;
import ham.arhat.config.support.AuthorizationResult;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

public class TransportImpl implements TransportConfig {
    private volatile ConfigTransport configTransport;
    private volatile KeyPair signingKeyPair;
    private volatile List<PublicKey> inboundPublicKeys;
    private volatile List<PublicKey> outboundPublicKeys;

    public void update(ConfigTransport configTransport) {
        this.configTransport = configTransport;
    }

    @Override
    public int getConnectTimeoutMillis() {
        return configTransport.getConnectTimeout();
    }

    @Override
    public int getListenPort() {
        return configTransport.getListenPort();
    }

    @Override
    public KeyPair getSigningKeyPair() {
        return signingKeyPair;
    }

    @Override
    public AuthorizationResult isAuthorizedKey(boolean incoming, byte[] encodedKey) {
        List<PublicKey> publicKeys = incoming ?
                inboundPublicKeys :
                outboundPublicKeys;

        for (PublicKey publicKey : publicKeys) {
            if (Arrays.equals(encodedKey, publicKey.getEncoded())) {
                return new AuthorizationResult(true, publicKey);
            }
        }

        return new AuthorizationResult(false, null);
    }

    public void updateKeys(KeyPair signingKeyPair, List<PublicKey> inboundPublicKeys, List<PublicKey> outboundPublicKeys) {
        this.signingKeyPair = signingKeyPair;
        this.inboundPublicKeys = inboundPublicKeys;
        this.outboundPublicKeys = outboundPublicKeys;
    }
}
