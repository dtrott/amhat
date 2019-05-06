package ham.arhat.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import static ham.arhat.message.TransportMessageType.CLOSE_ACK;
import static ham.arhat.message.TransportMessageType.CLOSE_FIN;

@Getter
public class CloseMessage implements TransportMessage {

    private final TransportMessageType type;

    public CloseMessage(TransportMessageType type) {
        if (!(type == CLOSE_FIN || type == CLOSE_ACK))
            throw new IllegalStateException("Invalid Message Type: " + type);

        this.type = type;
    }

    public TransportMessageType getType() {
        return type;
    }

    public IoBuffer encodeMessage() {

        return IoBuffer
                .allocate(1)
                .put((byte) getType().ordinal())
                .flip();
    }

    @Override
    public String toString() {
        return "CloseMessage(" + type + ")";
    }
}
