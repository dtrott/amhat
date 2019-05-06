package ham.arhat.hash.filter;

import ham.arhat.config.facade.TransportConfig;
import ham.arhat.config.support.AuthorizationResult;
import ham.arhat.hash.message.HandshakeMessage;
import ham.arhat.hash.message.HashMessage;
import ham.arhat.hash.message.HashMessageType;
import ham.arhat.hash.message.SecretsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterEvent;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RequiredArgsConstructor
public class HashHandler {
    private static final String GEN_DIFFIE_HELLMAN = "DH";

    private final TransportConfig transportConfig;
    private final boolean client;

    private IoBuffer inNetBuffer;
    private final Queue<IoFilterEvent> preHandshakeEventQueue = new ConcurrentLinkedQueue<>();

    private KeyPair dhKeyPair;
    private byte[] sharedSecret;

    private long rxCount = 0;
    private long txCount = 0;

    // Admin

    void sessionOpened(IoFilter.NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionOpened(session);

        writeFirstHandshakePacket(nextFilter, session);
    }

    // Inbound

    void messageReceived(IoFilter.NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            nextFilter.messageReceived(session, message);
            return;
        }

        IoBuffer ioBuffer = (IoBuffer) message;

        updateInBuffer(ioBuffer.buf(), buf -> processMessage(buf, nextFilter, session));
    }

    private void processMessage(IoBuffer input, IoFilter.NextFilter nextFilter, IoSession session) {
        try {
            while (input.prefixedDataAvailable(2)) {
                final int length = input.getUnsignedShort();
                HashMessageType type = HashMessageType.values()[input.getUnsigned()];

                switch (type) {
                    case HANDSHAKE:
                        processHandshakeMessage(input, nextFilter, session);
                        break;
                    case HASH:
                        processHashMessage(input, nextFilter, session);
                        break;
                    case SECRETS:
                        processSecrets(input);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processHandshakeMessage(IoBuffer input, IoFilter.NextFilter nextFilter, IoSession session) throws Exception {
        processHandshake(input, nextFilter, session);
        // Have we completed the handshake
        if (isComplete()) {
            flushPreHandshakeEvents(session);
        }
    }

    private void processHashMessage(IoBuffer input, IoFilter.NextFilter nextFilter, IoSession session) throws GeneralSecurityException {
        final HashMessage hashMessage = new HashMessage(this.sharedSecret, rxCount++, input);

        if (hashMessage.isDigestValid()) {
            nextFilter.messageReceived(session, IoBuffer.wrap(hashMessage.getPayload()));
        } else {
        }
    }

    private void processSecrets(IoBuffer input) {
        // Just read the bytes off the socket, then drop the message.
        new SecretsMessage(input);
    }

    void inputClosed(IoFilter.NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.inputClosed(session);
    }

    // Outbound

    void filterWrite(IoFilter.NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        if (isComplete()) {
            final Object message = writeRequest.getMessage();
            if (message instanceof Class) {
                if (SecretsMessage.class.equals(message))
                    nextFilter.filterWrite(session, processClassMessage(writeRequest));
            } else if (message instanceof IoBuffer) {
                nextFilter.filterWrite(session, hashWriteRequest(writeRequest));
            }
        } else {
            preHandshakeEventQueue.add(new IoFilterEvent(nextFilter, IoEventType.WRITE, session, writeRequest));
        }
    }

    private WriteRequest hashWriteRequest(WriteRequest writeRequest) throws NoSuchAlgorithmException {
        final IoBuffer buffer = (IoBuffer) writeRequest.getMessage();

        // Pass through write requests containing empty buffers.
        if (!buffer.hasRemaining())
            return writeRequest;

        buffer.mark();
        final byte[] buf = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit());

        final IoBuffer hashedMessage = new HashMessage(
                this.sharedSecret, txCount++, buf)
                .encodeMessage();

        return new HashedWriteRequest(hashedMessage, writeRequest);
    }

    void messageSent(IoFilter.NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        if (writeRequest instanceof HandshakeWriteRequest)
            return;

        if (writeRequest instanceof HashedWriteRequest) {
            nextFilter.messageSent(session, ((HashedWriteRequest) writeRequest).getParentRequest());
            return;
        }

        nextFilter.messageSent(session, writeRequest);
    }

    void filterClose(IoFilter.NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.filterClose(session);
    }

    void sessionClosed(IoFilter.NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionClosed(session);
    }

    // Misc

    void sessionIdle(IoFilter.NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        nextFilter.sessionIdle(session, status);
    }

    void exceptionCaught(IoFilter.NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    private void flushPreHandshakeEvents(IoSession session) throws Exception {
        IoFilterEvent scheduledWrite;

        while ((scheduledWrite = preHandshakeEventQueue.poll()) != null) {
            filterWrite(scheduledWrite.getNextFilter(), session, (WriteRequest) scheduledWrite.getParameter());
        }
    }

    private void updateInBuffer(ByteBuffer buf, IoBufferCallback callback) throws GeneralSecurityException {
        if (inNetBuffer == null) {
            inNetBuffer = IoBuffer.allocate(buf.remaining()).setAutoExpand(true);
        }

        inNetBuffer.put(buf);
        inNetBuffer.flip();

        if (!inNetBuffer.hasRemaining()) {
            inNetBuffer.free();
            inNetBuffer = null;
            return;
        }

        callback.process(inNetBuffer);

        // prepare to be written again
        if (inNetBuffer.hasRemaining()) {
            inNetBuffer.compact();
        } else {
            inNetBuffer.free();
            inNetBuffer = null;
        }
    }

    private WriteRequest processClassMessage(WriteRequest parent) {
        return new HashedWriteRequest(
                new SecretsMessage(dhKeyPair.getPrivate().getEncoded(), sharedSecret).encodeMessage(), parent);
    }

    //// Connect Handshake Code

    private boolean isComplete() {
        return sharedSecret != null;
    }

    private void writeFirstHandshakePacket(IoFilter.NextFilter nextFilter, IoSession session) throws GeneralSecurityException {
        // Only initialize from the client side.
        if (!client)
            return;

        this.dhKeyPair = generateDhKeyPair(x -> x.initialize(2048));
        sendHandshake(nextFilter, session);
    }

    private void processHandshake(IoBuffer input, IoFilter.NextFilter nextFilter, IoSession session) throws GeneralSecurityException {
        final HandshakeMessage handshakeMessage = new HandshakeMessage(input);

        // Clients create outgoing connections.
        final boolean incoming = !this.client;
        final AuthorizationResult result = transportConfig.isAuthorizedKey(incoming, handshakeMessage.getPublicKey());

        if (!result.isAuthorized())
            return;

        // TODO improve handling
        if (!handshakeMessage.isDigestValid(result.getPublicKey()))
            return;

        final DHPublicKey remotePublicKey = decodeDHPublicKey(handshakeMessage.getDhExchange());

        if (!client) {
            // Generate local keypair and send to remote.
            this.dhKeyPair = generateDhKeyPair(x -> x.initialize(remotePublicKey.getParams()));
            sendHandshake(nextFilter, session);
        }

        this.sharedSecret = generateSharedSecret(this.dhKeyPair.getPrivate(), remotePublicKey);
    }

    private void sendHandshake(IoFilter.NextFilter nextFilter, IoSession session) throws GeneralSecurityException {

        final KeyPair signingKeyPair = transportConfig.getSigningKeyPair();

        final IoBuffer message = new HandshakeMessage(
                this.dhKeyPair.getPublic().getEncoded(), signingKeyPair)
                .encodeMessage();

        nextFilter.filterWrite(session, new HandshakeWriteRequest(message));
    }

    private static KeyPair generateDhKeyPair(KeyPairInitializer initFunction) throws GeneralSecurityException {
        final KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(GEN_DIFFIE_HELLMAN);
        initFunction.initialize(keyPairGen);
        return keyPairGen.generateKeyPair();
    }

    private static DHPublicKey decodeDHPublicKey(byte[] keyPairEnc) throws GeneralSecurityException {
        final KeyFactory keyFactory = KeyFactory.getInstance(GEN_DIFFIE_HELLMAN);
        final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyPairEnc);
        return (DHPublicKey) keyFactory.generatePublic(x509KeySpec);
    }

    private static byte[] generateSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
        final KeyAgreement keyAgree = KeyAgreement.getInstance(GEN_DIFFIE_HELLMAN);
        keyAgree.init(privateKey);
        keyAgree.doPhase(publicKey, true);
        return keyAgree.generateSecret();
    }

    // Utility Interfaces

    private interface IoBufferCallback {
        void process(IoBuffer buffer) throws GeneralSecurityException;
    }

    private interface KeyPairInitializer {
        void initialize(KeyPairGenerator keyPairGen) throws InvalidAlgorithmParameterException;
    }

    private static class HandshakeWriteRequest extends DefaultWriteRequest {
        public HandshakeWriteRequest(Object message) {
            super(message);
        }

        @Override
        public boolean isEncoded() {
            return true;
        }
    }

    private static class HashedWriteRequest extends WriteRequestWrapper {
        private final IoBuffer message;

        public HashedWriteRequest(IoBuffer message, WriteRequest parentRequest) {
            super(parentRequest);
            this.message = message;
        }

        @Override
        public Object getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "HashedWriteRequest parent = " + this.getParentRequest().toString();
        }
    }
}
