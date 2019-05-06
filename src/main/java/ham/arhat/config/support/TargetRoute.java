package ham.arhat.config.support;

import lombok.Data;

import java.net.InetSocketAddress;

/**
 * Represents a route for a TCP connection.
 */
@Data
public class TargetRoute {
    /**
     * True if the route is valid.
     */
    private final boolean valid;

    /**
     * Username used as part of the authentication to the remote node.
     */
    private final String authenticationUser;

    /**
     * The first hop (Arhat node) for the connection.
     */
    private final InetSocketAddress remoteNode;

    /**
     * The final endpoint of the route.
     */
    private final EndPoint endPoint;
}
