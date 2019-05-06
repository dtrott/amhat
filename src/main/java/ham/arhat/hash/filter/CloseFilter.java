package ham.arhat.hash.filter;

import ham.arhat.hash.message.SecretsMessage;
import ham.arhat.message.CloseMessage;
import ham.arhat.message.TransportMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

import static ham.arhat.message.TransportMessageType.CLOSE_ACK;
import static ham.arhat.message.TransportMessageType.CLOSE_FIN;

/**
 * Adds special handling to allow secrets to be sent before closing the connection
 */
@Slf4j
@RequiredArgsConstructor
public class CloseFilter extends IoFilterAdapter {

    private final boolean client;

    // Close status of the incoming receive channel
    private static String ATTRIB_RECEIVE_CLOSE = "receiveClose";

    // Close status of the outbound send channel
    private static String ATTRIB_SEND_CLOSE = "sendClose";

    @Override
    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        // TODO
        nextFilter.sessionIdle(session, status);
    }

    // Message Handling

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
        if (message instanceof CloseMessage)
            closeMessageReceived(nextFilter, session, ((CloseMessage) message).getType());
        else if (session.getAttribute(ATTRIB_RECEIVE_CLOSE) == null)
            // Don't forward any messages after receiving a close
            nextFilter.messageReceived(session, message);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        // Don't forward close messages or special class messages
        final Object message = writeRequest.getMessage();
        if (!(message instanceof CloseMessage || message instanceof Class))
            nextFilter.messageSent(session, writeRequest);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        if (writeRequest.getMessage() == CloseMessage.class) {
            // Only send the close once.
            if (session.getAttribute(ATTRIB_SEND_CLOSE) != null)
                return;

            writeMessage(nextFilter, session, new CloseMessage(CLOSE_FIN), () -> {
                // Check someone else didn't jump in.
                if (session.getAttribute(ATTRIB_SEND_CLOSE) == null)
                    session.setAttribute(ATTRIB_SEND_CLOSE, CLOSE_FIN);
            });

        } else if (session.getAttribute(ATTRIB_SEND_CLOSE) == null)
            // Don't forward messages after we have started closing the socket.
            nextFilter.filterWrite(session, writeRequest);
    }

    // Close Handling

    @Override
    public void inputClosed(NextFilter nextFilter, IoSession session) throws Exception {
        hardClose(nextFilter, session);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        hardClose(nextFilter, session);
    }

    private void closeMessageReceived(NextFilter nextFilter, IoSession session, TransportMessageType messageType) {

        switch (messageType) {
            case CLOSE_FIN:
                session.setAttribute(ATTRIB_RECEIVE_CLOSE, CLOSE_FIN);
                // Let the app know that the remote side is closing.
                nextFilter.inputClosed(session);

                writeMessage(nextFilter, session, new CloseMessage(CLOSE_ACK), () -> {
                    session.setAttribute(ATTRIB_RECEIVE_CLOSE, CLOSE_ACK);
                    finalizeConnection(nextFilter, session, ATTRIB_SEND_CLOSE);
                });

                break;
            case CLOSE_ACK: {
                final TransportMessageType current = (TransportMessageType) session.getAttribute(ATTRIB_SEND_CLOSE);

                if (current != CLOSE_FIN) {
                    log.error("Protocol Violation, received CLOSE_ACK when current state is: " + current);
                    return;
                }
                session.setAttribute(ATTRIB_SEND_CLOSE, CLOSE_ACK);
                finalizeConnection(nextFilter, session, ATTRIB_RECEIVE_CLOSE);
                break;
            }
        }
    }

    private void finalizeConnection(NextFilter nextFilter, IoSession session, String validateAttribute) {
        if (CLOSE_ACK != session.getAttribute(validateAttribute))
            return;

        // Both sides have acknowledged the close so write out the secrets and close the connection.
        writeMessage(nextFilter, session, SecretsMessage.class, () -> {
            // We don't care if the write succeeds we are going to close either way.

            nextFilter.filterClose(session);
        });
    }

    private void writeMessage(NextFilter nextFilter, IoSession session, Object message, Runnable callback) {
        IoFutureListener<WriteFuture> listener = x -> {
            callback.run();
        };
        final DefaultWriteFuture future = new DefaultWriteFuture(session);
        future.addListener(listener);
        nextFilter.filterWrite(session, new DefaultWriteRequest(message, future));
    }

    private void hardClose(NextFilter nextFilter, IoSession session) {
        session.setAttribute(ATTRIB_RECEIVE_CLOSE, CLOSE_ACK);
        session.setAttribute(ATTRIB_SEND_CLOSE, CLOSE_ACK);

        session.closeNow();
        nextFilter.sessionClosed(session);
    }
}
