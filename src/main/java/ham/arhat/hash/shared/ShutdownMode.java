package ham.arhat.hash.shared;

public enum ShutdownMode {
    /**
     * The transport sent the correct sequence indicating a clean (1/2 closed) socket shutdown.
     */
    CLEAN,

    /**
     * The socket on the application side was closed.
     */
    APPLICATION,

    /**
     * The transport socket was hard closed - no data can be transferred
     */
    TRANSPORT
}
