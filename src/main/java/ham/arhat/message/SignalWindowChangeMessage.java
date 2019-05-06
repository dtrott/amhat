package ham.arhat.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

@Getter
public class SignalWindowChangeMessage implements SshCommandMessage {

    private final int commandId;
    private final int columns;
    private final int lines;

    /**
     * Build outgoing window change (from source).
     */
    public SignalWindowChangeMessage(int commandId, int columns, int lines) {
        this.commandId = commandId;
        this.columns = columns;
        this.lines = lines;
    }

    /**
     * Validate incoming handshake.
     */
    public SignalWindowChangeMessage(IoBuffer input) {
        this.commandId = input.getUnsignedShort();
        this.columns = input.getUnsignedShort();
        this.lines = input.getUnsignedShort();
    }

    @Override
    public TransportMessageType getType() {
        return TransportMessageType.SSH_SIGNAL_WINDOW_CHANGE;
    }

    public IoBuffer encodeMessage() {
        int totalLength = 7;

        return IoBuffer
                .allocate(totalLength)
                .put((byte) getType().ordinal())
                .putUnsignedShort(commandId)
                .putUnsignedShort(columns)
                .putUnsignedShort(lines)
                .flip();
    }
}
