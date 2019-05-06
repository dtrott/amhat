package ham.arhat.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

@Getter
public class DestroyCommandMessage implements SshCommandMessage {

    private final int commandId;

    /**
     * Build outgoing message.
     */
    public DestroyCommandMessage(int commandId) {
        this.commandId = commandId;
    }

    /**
     * Parse incoming message.
     */
    public DestroyCommandMessage(IoBuffer input) {
        this.commandId = input.getUnsignedShort();
    }

    @Override
    public TransportMessageType getType() {
        return TransportMessageType.SSH_DESTROY_COMMAND;
    }

    public IoBuffer encodeMessage() {

        return IoBuffer
                .allocate(3)
                .put((byte) getType().ordinal())
                .putUnsignedShort(commandId).flip();
    }
}
