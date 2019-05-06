package ham.arhat.config.impl;

import ham.arhat.config.facade.SshServerConfig;
import ham.arhat.config.file.ConfigSshServer;
import lombok.Getter;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;

@Getter
public class SshServerImpl implements SshServerConfig {

    private volatile int listenPort;
    private volatile KeyPairProvider hostKeyPairs;
    private volatile PublickeyAuthenticator publicKeyAuthenticator;

    public void update(ConfigSshServer sshServer) {
        this.listenPort = sshServer.getListenPort();
    }

    public void updateKeys(FileKeyPairProvider hostKeyPairs, PublickeyAuthenticator publickeyAuthenticator) {
        this.hostKeyPairs = hostKeyPairs;
        this.publicKeyAuthenticator = publickeyAuthenticator;
    }
}
