package ham.arhat.hash.filter;

import ham.arhat.config.facade.TransportConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;

@Slf4j
@AllArgsConstructor
public class HashIoFilter extends IoFilterAdapter {

    private static final String HASH_HANDLER = "hashHandler";

    private final TransportConfig transportConfig;
    private final boolean client;

    // Admin

    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    @Override
    public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionCreated(session);
    }

    @Override
    public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        final HashHandler hashHandler = new HashHandler(transportConfig, client);
        session.setAttribute(HASH_HANDLER, hashHandler);
        hashHandler.sessionOpened(nextFilter, session);
    }

    // Inbound

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        forwardCall(session, x -> x.messageReceived(nextFilter, session, message));
    }

    @Override
    public void inputClosed(NextFilter nextFilter, IoSession session) throws Exception {
        forwardCall(session, x -> x.inputClosed(nextFilter, session));
    }

    // Outbound

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        forwardCall(session, x -> x.filterWrite(nextFilter, session, writeRequest));
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        forwardCall(session, x -> x.messageSent(nextFilter, session, writeRequest));
    }

    @Override
    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        forwardCall(session, x -> x.filterClose(nextFilter, session));
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        forwardCall(session, x -> x.sessionClosed(nextFilter, session));
    }

    // Misc

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        forwardCall(session, x -> x.sessionIdle(nextFilter, session, status));
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        forwardCall(session, x -> x.exceptionCaught(nextFilter, session, cause));
    }

    private void forwardCall(IoSession session, ForwardedCall call) throws Exception {
        final Object attribute = session.getAttribute(HASH_HANDLER);
        if (attribute instanceof HashHandler) {
            call.forward((HashHandler) attribute);
        }
    }

    private interface ForwardedCall {
        void forward(HashHandler t) throws Exception;
    }
}
