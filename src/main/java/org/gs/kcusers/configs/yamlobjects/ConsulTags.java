package org.gs.kcusers.configs.yamlobjects;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "consul.service")
@Data
public class ConsulTags {
    final private List<String> tags = new ArrayList<>();
}
