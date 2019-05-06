package ham.arhat.config.support;

import lombok.Data;

import java.security.PublicKey;

/**
 * Result of an authorization operation.
 */
@Data
public class AuthorizationResult {
    /**
     * True if the operation succeeded / the action is authorized.
     */
    private final boolean authorized;

    /**
     * Contains the public key associated with the successful operation.
     */
    private final PublicKey publicKey;
}
