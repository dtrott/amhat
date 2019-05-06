package ham.arhat.hash.client;

import ham.arhat.config.facade.ConfigurationFactory;
import ham.arhat.config.facade.TransportConfig;
import ham.arhat.config.support.TargetRoute;
import ham.arhat.control.ControlConnections;
import ham.arhat.hash.filter.CloseFilter;
import ham.arhat.hash.filter.HashIoFilter;
import ham.arhat.hash.filter.SockTranDecoder;
import ham.arhat.hash.filter.SockTranEncoder;
import lombok.RequiredArgsConstructor;
import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.sshd.common.session.Session;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class HashClientManager {

    private final ConfigurationFactory configFactory;
    private final ControlConnections controlConnections;

    public void connect(Session sshSession, TargetRoute targetRoute) {
        final TransportConfig transportConfig = configFactory.getTransportConfig();

        try {
            NioSocketConnector connector = new NioSocketConnector();
            connector.setConnectTimeoutMillis(transportConfig.getConnectTimeoutMillis());

            final DefaultIoFilterChainBuilder filterChain = connector.getFilterChain();
            filterChain.addLast("secure", new HashIoFilter(transportConfig, true));
            filterChain.addLast("protocol", new ProtocolCodecFilter(new SockTranEncoder(), new SockTranDecoder()));
            filterChain.addLast("close", new CloseFilter(true));

            connector.setHandler(controlConnections);

            for (; ; ) {
                try {
                    //(IoSession session, ConnectFuture future)
                    ConnectFuture future = connector.connect(
                            targetRoute.getRemoteNode(),
                            (newSession, x) -> controlConnections.initializeHashSession(targetRoute, sshSession, newSession));
                    future.awaitUninterruptibly();
                    break;
                } catch (RuntimeIoException e) {
                    System.err.println("Failed to connect.");
                    e.printStackTrace();
                    Thread.sleep(5000);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
