package ham.arhat.config.loader;

import ham.arhat.config.file.Config;
import ham.arhat.config.file.ConfigPrivateKey;
import ham.arhat.config.file.ConfigSshClient;
import ham.arhat.config.file.ConfigSshServer;
import ham.arhat.config.file.ConfigTransport;
import ham.arhat.config.impl.SshClientImpl;
import ham.arhat.config.impl.SshServerImpl;
import ham.arhat.config.impl.TransportImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.util.security.bouncycastle.BouncyCastleKeyPairResourceParser;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class KeyLoader {

    private final SshServerImpl sshServerImpl;
    private final TransportImpl transportImpl;
    private final SshClientImpl sshClientImpl;

    public void loadKeys(Config config) {
        loadSshServerKeys(config.getSshServer());

        loadTransportKeys(config.getTransport());

        loadSshClientKeys(config.getSshClient());
    }

    private void loadSshServerKeys(ConfigSshServer configSshServer) {
        final List<Path> hostKeyPaths = configSshServer.getHostKeys()
                .stream()
                .map(Paths::get)
                .collect(Collectors.toList());

        final FileKeyPairProvider hostKeys = new FileKeyPairProvider(hostKeyPaths);

        final PublickeyAuthenticator publickeyAuthenticator = new AuthorizedKeysAuthenticator(Paths.get(configSshServer.getAuthorizedKeys()));

        sshServerImpl.updateKeys(hostKeys, publickeyAuthenticator);
    }

    private void loadTransportKeys(ConfigTransport configTransport) {
        try {
            final KeyPair signingKeyPair = loadKeyPair(configTransport.getSigningKey());
            final List<PublicKey> inboundPublicKeys = loadPublicKeys(configTransport.getInboundAuthorizedKeys());
            final List<PublicKey> outboundPublicKeys = loadPublicKeys(configTransport.getOutboundAuthorizedKeys());

            transportImpl.updateKeys(signingKeyPair, inboundPublicKeys, outboundPublicKeys);
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void loadSshClientKeys(ConfigSshClient sshClient) {
        final List<KeyPair> publicKeyIdentities = loadKeyPairs(sshClient.getPrivateKeys());

        sshClientImpl.updateKeys(publicKeyIdentities);
    }

    private static List<PublicKey> loadPublicKeys(String pathAuthorizedKeys) throws IOException, GeneralSecurityException {
        final List<AuthorizedKeyEntry> entries = AuthorizedKeyEntry.readAuthorizedKeys(Paths.get(pathAuthorizedKeys));

        return PublicKeyEntry.resolvePublicKeyEntries(null, entries, null);
    }

    private static List<KeyPair> loadKeyPairs(Collection<ConfigPrivateKey> privateKeys) {
        final List<KeyPair> keyPairs = new ArrayList<>();
        for (ConfigPrivateKey privateKey : privateKeys) {
            try {
                keyPairs.add(loadKeyPair(privateKey));
            } catch (IOException | GeneralSecurityException e) {
                log.warn(e.getMessage(), e);
            }
        }

        return keyPairs;
    }

    private static KeyPair loadKeyPair(ConfigPrivateKey privateKey) throws IOException, GeneralSecurityException {
        return loadKeyPair(privateKey.getPath(), privateKey.getPassword());
    }

    private static KeyPair loadKeyPair(String path, String password) throws IOException, GeneralSecurityException {
        final FilePasswordProvider passwordProvider = FilePasswordProvider.of(password);
        Path keyPath = Paths.get(path);

        final BouncyCastleKeyPairResourceParser parser = new BouncyCastleKeyPairResourceParser();

        final SessionContext session = null;
        final Collection<KeyPair> keyPairs = parser.loadKeyPairs(session, keyPath, passwordProvider);

        return keyPairs.stream().findFirst().get();
    }
}
