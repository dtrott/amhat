package ham.arhat.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;

@Getter
public class CommandExitedMessage implements SshCommandMessage {

    private final int commandId;
    private final int exitValue;
    private final String exitMessage;

    /**
     * Build outgoing message.
     */
    public CommandExitedMessage(int commandId, int exitValue, String exitMessage) {
        this.commandId = commandId;
        this.exitValue = exitValue;
        this.exitMessage = exitMessage == null ? "" : exitMessage;
    }

    /**
     * Parse incoming message.
     */
    public CommandExitedMessage(IoBuffer input) throws CharacterCodingException {
        this.commandId = input.getUnsignedShort();
        this.exitValue = input.get();
        this.exitMessage = input.getPrefixedString(MESSAGE_CHARSET.newDecoder());
    }

    @Override
    public TransportMessageType getType() {
        return TransportMessageType.SSH_COMMAND_EXITED;
    }

    public IoBuffer encodeMessage() throws CharacterCodingException {
        return IoBuffer
                .allocate(6)
                .setAutoExpand(true)
                .put((byte) getType().ordinal())
                .putUnsignedShort(commandId)
                .put((byte) exitValue)
                .putPrefixedString(exitMessage, MESSAGE_CHARSET.newEncoder())
                .flip();
    }
}
