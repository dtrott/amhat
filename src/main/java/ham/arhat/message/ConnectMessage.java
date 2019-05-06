package ham.arhat.message;

import ham.arhat.config.support.EndPoint;
import ham.arhat.config.support.TargetRoute;
import lombok.Getter;
import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

@Getter
public class ConnectMessage implements TransportMessage {

    private final String authenticationUser;
    private final String remoteUsername;
    private final String remoteHostname;
    private final int remotePort;

    public ConnectMessage(TargetRoute targetRoute) {
        final EndPoint endPoint = targetRoute.getEndPoint();

        this.authenticationUser = targetRoute.getAuthenticationUser();
        this.remoteUsername = endPoint.getUsername();
        this.remoteHostname = endPoint.getHostname();
        this.remotePort = endPoint.getPort();
    }

    /**
     * Build outgoing message.
     */
    public ConnectMessage(String authenticationUser, String remoteUsername, String remoteHostname, int remotePort) {
        this.authenticationUser = authenticationUser;
        this.remoteUsername = remoteUsername;
        this.remoteHostname = remoteHostname;
        this.remotePort = remotePort;
    }

    /**
     * Parse incoming message.
     */
    public ConnectMessage(IoBuffer input) throws CharacterCodingException {
        final CharsetDecoder decoder = MESSAGE_CHARSET.newDecoder();

        this.authenticationUser = input.getPrefixedString(decoder);
        this.remoteUsername = input.getPrefixedString(decoder);
        this.remoteHostname = input.getPrefixedString(decoder);
        this.remotePort = input.getUnsignedShort();
    }

    public TransportMessageType getType() {
        return TransportMessageType.SSH_CONNECT;
    }

    public IoBuffer encodeMessage() throws CharacterCodingException {
        final CharsetEncoder encoder = MESSAGE_CHARSET.newEncoder();

        return IoBuffer
                .allocate(9)
                .setAutoExpand(true)
                .put((byte) getType().ordinal())
                .putPrefixedString(authenticationUser, encoder)
                .putPrefixedString(remoteUsername, encoder)
                .putPrefixedString(remoteHostname, encoder)
                .putUnsignedShort(remotePort)
                .flip();
    }
}
