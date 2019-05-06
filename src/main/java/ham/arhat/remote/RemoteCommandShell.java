package ham.arhat.remote;

import ham.arhat.message.StartCommandMessage;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.Environment;

import java.io.IOException;
import java.util.Map;

public class RemoteCommandShell extends RemoteCommand<ChannelShell> {

    public RemoteCommandShell(RemoteConnection connection, StartCommandMessage startCommand) {
        super(connection, startCommand);
    }

    @Override
    protected ChannelShell openChannel(ClientSession session, String command) throws IOException {
        return session.createShellChannel();
    }

    @Override
    protected void configurePty(final Map<String, String> env, ChannelShell channel) {

        final String type = env.get(Environment.ENV_TERM);
        if (type != null)
            channel.setPtyType(type);

        final String columns = env.get(Environment.ENV_COLUMNS);
        if (columns != null)
            channel.setPtyColumns(Integer.parseInt(columns));

        final String lines = env.get(Environment.ENV_LINES);
        if (lines != null)
            channel.setPtyLines(Integer.parseInt(lines));
    }

    @Override
    protected void sendWindowChange(int columns, int lines) {
        try {
            if (this.channel != null)
                this.channel.sendWindowChange(columns, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
