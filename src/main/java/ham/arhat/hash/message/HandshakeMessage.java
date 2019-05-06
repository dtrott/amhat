package ham.arhat.hash.message;

import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;

import static ham.arhat.hash.message.MessageUtil.readShortBytes;

@Getter
public class HandshakeMessage {

    private static final String GEN_DIFFIE_HELLMAN = "DH";
    private static final String HASH_SHA_256 = "SHA-256";
    private static final String CIPHER_RSA = "RSA";

    private byte[] dhExchange;
    private byte[] publicKey;

    private byte[] signature;

    private byte[] digest;

    /**
     * Build outgoing handshake (from source).
     */
    public HandshakeMessage(byte[] dhExchange, KeyPair signingKeyPair) throws GeneralSecurityException {
        this.dhExchange = dhExchange;
        this.publicKey = signingKeyPair.getPublic().getEncoded();

        this.digest = calculateDigest(this.dhExchange);
        this.signature = encrypt(signingKeyPair.getPrivate(), this.digest);
    }

    /**
     * Validate incoming handshake.
     */
    public HandshakeMessage(IoBuffer input) throws GeneralSecurityException {
        this.dhExchange = readShortBytes(input);
        this.publicKey = readShortBytes(input);

        this.signature = readShortBytes(input);
        this.digest = calculateDigest(this.dhExchange);
    }

    public IoBuffer encodeMessage() {
        int totalLength = dhExchange.length + publicKey.length + signature.length + 7;

        return IoBuffer
                .allocate(totalLength + 2)
                .setAutoExpand(true)
                .putShort((short) totalLength)
                .put((byte) HashMessageType.HANDSHAKE.ordinal())
                .putShort((short) dhExchange.length)
                .put(dhExchange)
                .putShort((short) publicKey.length)
                .put(publicKey)
                .putShort((short) signature.length)
                .put(signature)
                .flip();
    }

    public boolean isDigestValid(PublicKey key) throws GeneralSecurityException {
        return Arrays.equals(this.digest, decrypt(key, this.signature));
    }

    private static byte[] calculateDigest(byte[] dhExchange) throws NoSuchAlgorithmException {
        final MessageDigest msgDigest = MessageDigest.getInstance(HASH_SHA_256);
        return msgDigest.digest(dhExchange);
    }

    private static byte[] encrypt(Key privateKey, byte[] bytes) throws GeneralSecurityException {
        final String algorithm = privateKey.getAlgorithm();
        final Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(bytes);
    }

    private static byte[] decrypt(Key publicKey, byte[] encrypted) throws GeneralSecurityException {
        final String algorithm = publicKey.getAlgorithm();
        final Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        return cipher.doFinal(encrypted);
    }
}
