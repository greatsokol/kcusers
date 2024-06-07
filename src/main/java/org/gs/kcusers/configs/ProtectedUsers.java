package org.gs.kcusers.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "service.keycloakclient.inactivity")
public class ProtectedUsers {
    final private List<String> protectedusers = new ArrayList<String>();

    public List<String> getProtectedUsers() {
        return protectedusers;
    }
}
