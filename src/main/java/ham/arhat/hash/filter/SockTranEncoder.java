package ham.arhat.hash.filter;

import ham.arhat.message.TransportMessage;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class SockTranEncoder extends ProtocolEncoderAdapter {
    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (message instanceof Class) {
            // Pass class messages straight through
            out.write(message);
        } else if (message instanceof TransportMessage) {
            out.write(((TransportMessage) message).encodeMessage());
        }
    }
}
