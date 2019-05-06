package ham.arhat.config.facade;

import ham.arhat.config.support.TargetRoute;

/**
 * Configuration specific to the control node.
 */
public interface ControlConfig {
    /**
     * Looks up the desired target based on the SSH username provided by the client.
     *
     * @param username the username provided as part of the incoming SSH connection.
     * @return the route to the target (the valid field will be false, if no valid route is found).
     */
    TargetRoute getTargetRoute(String username);
}
