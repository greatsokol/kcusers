package org.gs.kcusers.configs.yamlobjects;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "service.keycloakclient.inactivity")
@Data
public class ProtectedUsers {
    final private List<String> protectedusers = new ArrayList<>();
}
