package org.gs.kcusers.configs.vault;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "vault")
@Data
public class VaultEngines {
    private Map<String, Map<String, Map<String, String>>> engines;

    boolean isEmpty() {
        return engines.isEmpty();
    }

    Map<String, Map<String, Map<String, String>>> getEngines() {
        return engines;
    }
}
