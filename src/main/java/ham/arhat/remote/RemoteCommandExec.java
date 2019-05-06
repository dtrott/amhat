package ham.arhat.remote;

import ham.arhat.message.StartCommandMessage;
import org.apache.sshd.client.channel.ChannelSession;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;

import java.io.IOException;
import java.util.Map;

public class RemoteCommandExec extends RemoteCommand {

    public RemoteCommandExec(RemoteConnection connection, StartCommandMessage startCommand) {
        super(connection, startCommand);
    }

    @Override
    protected ChannelSession openChannel(ClientSession session, String command) throws IOException {
        return session.createExecChannel(command);
    }

    @Override
    protected void configurePty(Map env, ClientChannel channel) {
        // No Op - Window changes not supported by this command.
    }

    @Override
    protected void sendWindowChange(int columns, int lines) {
        // No Op - Window changes not supported by this command.
    }
}
