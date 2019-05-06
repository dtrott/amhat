package ham.arhat.ssh.server;

import ham.arhat.control.ControlCommand;
import ham.arhat.control.ControlConnections;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.server.ChannelSessionAware;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.AsyncCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
@Getter
public class CommandCapture implements AsyncCommand, ChannelSessionAware {

    private final ControlConnections controlConnections;
    private final CommandType commandType;
    private final String command;
    private ChannelSession session;
    private IoOutputStream out;
    private IoOutputStream err;
    private ExitCallback callback;
    private volatile ControlCommand controlCommand;

    @Override
    public void setChannelSession(ChannelSession session) {
        this.session = session;
    }

    @Override
    public void setIoInputStream(IoInputStream in) {
        // No-Op - using ChannelDataReceiver
    }

    @Override
    public void setIoOutputStream(IoOutputStream out) {
        this.out = out;
    }

    @Override
    public void setIoErrorStream(IoOutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        this.controlCommand = controlConnections.newControlCommand(this);
        if (this.controlCommand != null)
            this.controlCommand.start(env);
    }

    @Override
    public void destroy() throws Exception {
        if (this.controlCommand != null)
            this.controlCommand.destroy();
    }

    @Override
    public void setInputStream(InputStream in) {
        // No-Op - using ChannelDataReceiver
    }

    @Override
    public void setOutputStream(OutputStream out) {
        // No-Op Async
    }

    @Override
    public void setErrorStream(OutputStream err) {
        // No-Op Async
    }

}
