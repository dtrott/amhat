package ham.arhat.config.facade;

import java.security.KeyPair;
import java.util.Collection;

/**
 * Provides the configuration for outbound SSH connections from the remote Arhat node.
 */
public interface SshClientConfig {
    /**
     * Returns the list of public key identities offered to the remote SSH server in order to authenticate.
     */
    Collection<KeyPair> getPublicKeyIdentities();
}
