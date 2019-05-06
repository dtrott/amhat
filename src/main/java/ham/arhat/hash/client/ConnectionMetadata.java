package ham.arhat.hash.client;

import lombok.Data;
import org.apache.mina.core.session.IoSession;

@Data
public class ConnectionMetadata {
    private final long connectionId;
    private final IoSession session;
}
