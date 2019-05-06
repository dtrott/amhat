package ham.arhat.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import ham.arhat.config.loader.ConfigCache;
import ham.arhat.hash.server.HashServer;
import ham.arhat.remote.RemoteConnections;
import ham.arhat.ssh.server.SshServerManager;

public class Arhat {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new GuiceModule());

        final ConfigCache configCache = injector.getInstance(ConfigCache.class);
        final SshServerManager sshServerManager = injector.getInstance(SshServerManager.class);
        final HashServer hashServer = injector.getInstance(HashServer.class);
        final RemoteConnections remoteConnections = injector.getInstance(RemoteConnections.class);

        configCache.initialize();
        remoteConnections.initialize();
        sshServerManager.initialize();
        hashServer.initialize();
    }
}
