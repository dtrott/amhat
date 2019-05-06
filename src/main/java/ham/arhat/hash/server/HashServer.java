package ham.arhat.hash.server;

import ham.arhat.config.facade.ConfigurationFactory;
import ham.arhat.config.facade.TransportConfig;
import ham.arhat.hash.filter.CloseFilter;
import ham.arhat.hash.filter.HashIoFilter;
import ham.arhat.hash.filter.SockTranDecoder;
import ham.arhat.hash.filter.SockTranEncoder;
import ham.arhat.remote.RemoteConnections;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetSocketAddress;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Singleton
public class HashServer {

    private final ConfigurationFactory configFactory;
    private final RemoteConnections remoteConnections;

    public void initialize() {
        final TransportConfig transportConfig = configFactory.getTransportConfig();

        final int listenPort = transportConfig.getListenPort();

        // Don't start listening unless we have a valid port
        if (listenPort <= 0)
            return;

        try {
            IoAcceptor acceptor = new NioSocketAcceptor();

            final DefaultIoFilterChainBuilder filterChain = acceptor.getFilterChain();
            filterChain.addLast("secure", new HashIoFilter(transportConfig, false));
            filterChain.addLast("protocol", new ProtocolCodecFilter(new SockTranEncoder(), new SockTranDecoder()));
            filterChain.addLast("close", new CloseFilter(false));

            acceptor.setHandler(remoteConnections);

            acceptor.getSessionConfig().setReadBufferSize(2048);
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 60);

            acceptor.bind(new InetSocketAddress(listenPort));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
