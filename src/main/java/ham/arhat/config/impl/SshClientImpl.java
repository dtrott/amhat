package ham.arhat.config.impl;

import ham.arhat.config.facade.SshClientConfig;
import lombok.Getter;

import java.security.KeyPair;
import java.util.Collection;

@Getter
public class SshClientImpl implements SshClientConfig {

    private volatile Collection<KeyPair> publicKeyIdentities;

    public void updateKeys(Collection<KeyPair> publicKeyIdentities) {
        this.publicKeyIdentities = publicKeyIdentities;
    }
}
