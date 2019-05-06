package ham.arhat.message;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

public interface TransportMessage {
    Charset MESSAGE_CHARSET = Charset.forName("UTF8");

    TransportMessageType getType();

    IoBuffer encodeMessage() throws CharacterCodingException;

}
