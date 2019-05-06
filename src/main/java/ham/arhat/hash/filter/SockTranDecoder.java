package ham.arhat.hash.filter;

import ham.arhat.message.CloseMessage;
import ham.arhat.message.CommandExitedMessage;
import ham.arhat.message.ConnectMessage;
import ham.arhat.message.DestroyCommandMessage;
import ham.arhat.message.SendDataMessage;
import ham.arhat.message.SignalWindowChangeMessage;
import ham.arhat.message.StartCommandMessage;
import ham.arhat.message.TransportMessageType;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.nio.charset.CharacterCodingException;

public class SockTranDecoder extends ProtocolDecoderAdapter {

    @Override
    public void decode(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
        final TransportMessageType type = TransportMessageType.values()[buffer.getUnsigned()];

        final Object hydrated = hydrateMessage(buffer, type);
        if (hydrated != null)
            out.write(hydrated);
    }

    private Object hydrateMessage(IoBuffer buffer, TransportMessageType type) throws CharacterCodingException {
        switch (type) {
            case SSH_CONNECT:
                return new ConnectMessage(buffer);
            case SSH_START_COMMAND:
                return new StartCommandMessage(buffer);
            case SSH_SEND_DATA:
                return new SendDataMessage(buffer);
            case SSH_SIGNAL_WINDOW_CHANGE:
                return new SignalWindowChangeMessage(buffer);
            case SSH_DESTROY_COMMAND:
                return new DestroyCommandMessage(buffer);
            case SSH_COMMAND_EXITED:
                return new CommandExitedMessage(buffer);
            case CLOSE_FIN:
            case CLOSE_ACK:
                return new CloseMessage(type);
        }
        return null;
    }
}
