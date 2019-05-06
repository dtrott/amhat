package ham.arhat.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import static ham.arhat.hash.message.MessageUtil.readShortBytes;

@Getter
public class SendDataMessage implements SshCommandMessage {

    private final int commandId;
    private final DataChannel dataChannel;
    private final byte[] data;

    /**
     * Build outgoing handshake (from source).
     */
    public SendDataMessage(int commandId, DataChannel dataChannel, byte[] data) {
        this.commandId = commandId;
        this.dataChannel = dataChannel;
        this.data = data;
    }

    /**
     * Validate incoming handshake.
     */
    public SendDataMessage(IoBuffer input) {
        this.commandId = input.getUnsignedShort();
        this.dataChannel = DataChannel.values()[input.getUnsigned()];
        this.data = readShortBytes(input);
    }

    @Override
    public TransportMessageType getType() {
        return TransportMessageType.SSH_SEND_DATA;
    }

    public IoBuffer encodeMessage() {
        int totalLength = data.length + 6;

        return IoBuffer
                .allocate(totalLength)
                .put((byte) getType().ordinal())
                .putUnsignedShort(commandId)
                .put((byte) dataChannel.ordinal())
                .putUnsignedShort(data.length)
                .put(data)
                .flip();
    }
}
