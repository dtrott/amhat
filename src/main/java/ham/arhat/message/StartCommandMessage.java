package ham.arhat.message;

import ham.arhat.ssh.server.CommandType;
import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.util.Map;

import static ham.arhat.hash.message.MessageUtil.getStringMap;
import static ham.arhat.hash.message.MessageUtil.putStringMap;

@Getter
public class StartCommandMessage implements SshCommandMessage {

    private final CommandType commandType;
    private final int commandId;
    private final String commandLine;
    private final Map<String, String> environment;

    /**
     * Build outgoing message.
     */
    public StartCommandMessage(CommandType commandType, int commandId, String commandLine, Map<String, String> environment) {
        this.commandType = commandType;
        this.commandId = commandId;
        this.commandLine = commandLine;
        this.environment = environment;
    }

    /**
     * Parse incoming message.
     */
    public StartCommandMessage(IoBuffer input) throws CharacterCodingException {
        this.commandId = input.getUnsignedShort();
        this.commandType = CommandType.values()[input.getUnsigned()];
        if (hasCommandLine()) {
            this.commandLine = input.getPrefixedString(MESSAGE_CHARSET.newDecoder());
        } else
            this.commandLine = null;
        this.environment = getStringMap(input, MESSAGE_CHARSET);
    }

    @Override
    public TransportMessageType getType() {
        return TransportMessageType.SSH_START_COMMAND;
    }

    public IoBuffer encodeMessage() throws CharacterCodingException {
        final IoBuffer buffer = IoBuffer
                .allocate(4)
                .setAutoExpand(true)
                .put((byte) getType().ordinal())
                .putUnsignedShort(commandId)
                .put((byte) commandType.ordinal());

        if (hasCommandLine()) {
            buffer.putPrefixedString(commandLine, MESSAGE_CHARSET.newEncoder());
        }

        putStringMap(this.environment, buffer, MESSAGE_CHARSET);

        return buffer.flip();
    }

    private boolean hasCommandLine() {
        return this.commandType == CommandType.EXEC;
    }
}
