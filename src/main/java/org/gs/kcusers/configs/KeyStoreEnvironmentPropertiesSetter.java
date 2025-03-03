/*
 * Created by Eugene Sokolov 28.02.2025, 11:30.
 */

package org.gs.kcusers.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;

import static org.springframework.util.ResourceUtils.getFile;
import static org.springframework.util.ResourceUtils.isUrl;


@Component
public class KeyStoreEnvironmentPropertiesSetter {
    private static final Logger logger = LoggerFactory.getLogger(KeyStoreEnvironmentPropertiesSetter.class);

    @Value("${keyStore.path}:#{null}")
    String path;

    @Value("${keyStore.type}:#{null}")
    String type;

    @Value("${keyStore.password}:#{null}")
    String password;

    @PostConstruct
    public void setProperty() throws FileNotFoundException {
        if (path != null && !path.isEmpty() && type != null && !type.isEmpty()) {
            String absolutePath = path;
            logger.info("Environment properties set: keyStore: '{}', " +
                    "keyStoreType: '{}', " +
                    "keyStorePassword: '{}'", path, type, password);
            if (isUrl(path)) {
                absolutePath = getFile(path).getPath();
            }
            System.setProperty("javax.net.ssl.keyStore", absolutePath);
            System.setProperty("javax.net.ssl.keyStoreType", type);
            if (password != null && !password.isEmpty()) {
                System.setProperty("javax.net.ssl.keyStorePassword", password);
            }
        }
    }
}
