package ham.arhat.config.facade;

import ham.arhat.config.support.AuthorizationResult;

import java.security.KeyPair;

/**
 * Configuration for the inter-node transport.
 */
public interface TransportConfig {

    /**
     * The timeout when connecting to a remote node (in milliseconds).
     */
    int getConnectTimeoutMillis();

    /**
     * Returns the port that the Arnet server listens for incoming inter-node (hash) connections.
     *
     * @return the port number or zero if the port should not be opened.
     */
    int getListenPort();

    /**
     * Returns the key used to sign the handshake packet between two Arhat nodes.
     */
    KeyPair getSigningKeyPair();

    /**
     * Checks to see if the provided key is authorized to connect to this server.
     * <p>
     * If authorized, the result will contain the hydrated version of the public key,
     * that can be used to verify the digest/checksum of the handshake packet.
     *
     * @param incoming   true if this is an incoming connection, false if we are verifying an outgoing connection.
     * @param encodedKey an X.509 encoded key.
     * @return the result of the authorization check.
     */
    AuthorizationResult isAuthorizedKey(boolean incoming, byte[] encodedKey);
}
