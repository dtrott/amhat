package ham.arhat.remote;

import ham.arhat.message.StartCommandMessage;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.subsystem.sftp.SftpConstants;

import java.io.IOException;
import java.util.Map;

public class RemoteCommandSftp extends RemoteCommand<ClientChannel> {
    public RemoteCommandSftp(RemoteConnection connection, StartCommandMessage startCommand) {
        super(connection, startCommand);
    }

    @Override
    protected ClientChannel openChannel(ClientSession session, String command) throws IOException {
        return session.createChannel(Channel.CHANNEL_SUBSYSTEM, SftpConstants.SFTP_SUBSYSTEM_NAME);
    }

    @Override
    protected void configurePty(Map<String, String> env, ClientChannel channel) {
        // No Op - Window changes not supported by this command.
    }

    @Override
    protected void sendWindowChange(int columns, int lines) {
        // No Op - Window changes not supported by this command.
    }
}
