package ham.arhat.hash.filter;

import lombok.Data;

import java.security.PublicKey;

@Data
public class Authorization {
    private final PublicKey publicKey;
}
