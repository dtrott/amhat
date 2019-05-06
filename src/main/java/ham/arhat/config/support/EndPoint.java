package ham.arhat.config.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndPoint {
    private Type type = Type.SSH;
    private String username;
    private String hostname;
    private int port;

    public enum Type {
        SSH
    }
}
