package ham.arhat.message;

public interface SshCommandMessage extends TransportMessage {
    int getCommandId();
}
