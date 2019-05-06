package ham.arhat.config.file;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConfigSshServer {
    private int listenPort;
    private List<String> hostKeys;
    private String authorizedKeys;
    private Map<String, ConfigSshUser> users = new LinkedHashMap<>();
}
