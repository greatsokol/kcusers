package org.gs.kcusers.configs.vault;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "vault")
@Data
public class VaultPaths {
    private Map<String, Object> paths;

    boolean isEmpty() {
        return paths.isEmpty();
    }

    Map<String, Object> getPaths() {
        return paths;
    }
}
