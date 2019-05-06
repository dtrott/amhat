package ham.arhat.hash.message;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageUtil {
    public static byte[] readShortBytes(IoBuffer input) {
        final int length = input.getUnsignedShort();
        byte[] bytes = new byte[length];
        input.get(bytes);
        return bytes;
    }

    public static Map<String, String> getStringMap(IoBuffer input, Charset charset) {
        try {
            final CharsetDecoder decoder = charset.newDecoder();
            final Map<String, String> mapping = new LinkedHashMap<>();

            final int count = input.getUnsignedShort();
            for (int i = 0; i < count; i++) {
                mapping.put(
                        input.getPrefixedString(decoder),
                        input.getPrefixedString(decoder));
            }

            return mapping;
        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public static IoBuffer putStringMap(Map<String, String> mapping, IoBuffer output, Charset charset) {
        try {
            final CharsetEncoder encoder = charset.newEncoder();

            output.putUnsignedShort(mapping.size());
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                output.putPrefixedString(entry.getKey(), encoder);
                output.putPrefixedString(entry.getValue(), encoder);
            }
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }

        return output;
    }
}
