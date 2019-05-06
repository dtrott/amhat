package ham.arhat.ssh.server;

import ham.arhat.control.ControlConnections;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.common.subsystem.sftp.SftpConstants;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.subsystem.SubsystemFactory;

import javax.inject.Inject;
import java.util.Collections;

@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class CommandFactory {

    private final ControlConnections controlConnections;

    public void setSshFactories(SshServer sshServer) {
        sshServer.setShellFactory(this::newShellCommand);
        sshServer.setCommandFactory(this::newExecCommand);
        sshServer.setSubsystemFactories(Collections.singletonList(this.sftpFactory));
    }

    private Command newShellCommand() {
        return new CommandCapture(controlConnections, CommandType.SHELL, null);
    }

    private Command newExecCommand(String command) {
        return new CommandCapture(controlConnections, CommandType.EXEC, command);
    }

    private SubsystemFactory sftpFactory = new SubsystemFactory() {
        @Override
        public Command create() {
            return new CommandCapture(controlConnections, CommandType.SFTP, null);
        }

        @Override
        public String getName() {
            return SftpConstants.SFTP_SUBSYSTEM_NAME;
        }
    };
}
