package ham.arhat.hash.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static ham.arhat.hash.message.MessageUtil.readShortBytes;

@Getter
public class HashMessage {

    private static final String HASH_SHA_256 = "SHA-256";

    private byte[] sharedSecret;
    private long messageId;
    private byte[] payload;
    private byte[] digest;
    private boolean digestValid;

    // Build outbound message
    public HashMessage(byte[] sharedSecret, long messageId, byte[] payload) throws NoSuchAlgorithmException {
        this.sharedSecret = sharedSecret;
        this.messageId = messageId;

        this.payload = payload;
        this.digest = generateDigest();
        this.digestValid = true;
    }

    // Parse inbound message
    public HashMessage(byte[] sharedSecret, long messageId, IoBuffer input) throws NoSuchAlgorithmException {
        this.sharedSecret = sharedSecret;
        this.messageId = messageId;

        this.payload = readShortBytes(input);
        this.digest = readShortBytes(input);
        this.digestValid = Arrays.equals(this.digest, generateDigest());
    }

    public IoBuffer encodeMessage() throws NoSuchAlgorithmException {
        int total = payload.length + this.digest.length + 5;

        return IoBuffer
                .allocate(total + 2)
                .putShort((short) total)
                .put((byte) HashMessageType.HASH.ordinal())
                .putShort((short) payload.length)
                .put(payload)
                .putShort((short) digest.length)
                .put(digest)
                .flip();
    }

    private byte[] generateDigest() throws NoSuchAlgorithmException {
        final MessageDigest msgDigest = MessageDigest.getInstance(HASH_SHA_256);
        msgDigest.update(sharedSecret);
        msgDigest.update(ByteBuffer.allocate(8).putLong(messageId).array());
        msgDigest.update(payload);
        return msgDigest.digest();
    }
}
