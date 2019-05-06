package ham.arhat.config.facade;

import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;

public interface SshServerConfig {
    /**
     * Returns the port that the Arnet node listens for incoming SSH connections.
     *
     * @return the port or 0 if this node should not listen for SSH connections.
     */
    int getListenPort();

    /**
     * The provider of host key pairs.
     * <p>
     * These are provided to the client to authenticate the Arnet node.
     */
    KeyPairProvider getHostKeyPairs();

    /**
     * The authenticator used to authenticate incoming SSH connections.
     */
    PublickeyAuthenticator getPublicKeyAuthenticator();
}
