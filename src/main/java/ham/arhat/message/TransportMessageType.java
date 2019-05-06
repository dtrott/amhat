package ham.arhat.message;

public enum TransportMessageType {
    SSH_CONNECT,
    SSH_START_COMMAND,
    SSH_SEND_DATA,
    SSH_SIGNAL_WINDOW_CHANGE,
    SSH_DESTROY_COMMAND,
    SSH_COMMAND_EXITED,
    // High level simulation of the normal FIN / ACK done at the TCP level.
    CLOSE_FIN,
    CLOSE_ACK
}
