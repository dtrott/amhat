package ham.arhat.config.file;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class ConfigSshClient {
    private List<ConfigPrivateKey> privateKeys;
}
