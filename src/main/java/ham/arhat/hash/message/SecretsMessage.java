package ham.arhat.hash.message;

import org.apache.mina.core.buffer.IoBuffer;

import static ham.arhat.hash.message.MessageUtil.readShortBytes;

public class SecretsMessage {

    private byte[] dhPrivateKey;
    private byte[] sharedSecret;

    /**
     * Generate message from source
     */
    public SecretsMessage(byte[] dhPrivateKey, byte[] sharedSecret) {
        this.dhPrivateKey = dhPrivateKey;
        this.sharedSecret = sharedSecret;
    }

    public SecretsMessage(IoBuffer input) {
        this.dhPrivateKey = readShortBytes(input);
        this.sharedSecret = readShortBytes(input);
    }

    public IoBuffer encodeMessage() {
        int total = dhPrivateKey.length + this.sharedSecret.length + 5;

        return IoBuffer
                .allocate(total + 2)
                .putUnsignedShort(total)
                .put((byte) HashMessageType.SECRETS.ordinal())
                .putUnsignedShort(dhPrivateKey.length)
                .put(dhPrivateKey)
                .putUnsignedShort(sharedSecret.length)
                .put(sharedSecret)
                .flip();
    }
}
