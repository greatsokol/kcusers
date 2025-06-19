package org.gs.kcusers.configs.vault;


import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class VaultConfig {
    private static final Logger logger = LoggerFactory.getLogger(VaultConfig.class);

    @Value("${vault.enabled:false}")
    boolean enabled;

    @Value("${vault.uri:#{null}}")
    String uri;

    @Value("${vault.ssl.type:#{null}}")
    String sslType;

    @Value("${vault.ssl.certificate:#{null}}")
    String sslCertificate;

    @Value("${vault.ssl.key:#{null}}")
    String sslKeyPath;

    @Value("${vault.ssl.key-store:#{null}}")
    String sslKeyStore;

    @Value("${vault.ssl.key-store-type:#{null}}")
    String sslKeyStoreType;

    @Value("${vault.ssl.key-store-password:#{null}}")
    String sslKeyStorePassword;


    private final VaultPaths paths;

    @Autowired
    VaultConfig(VaultPaths paths) {
        this.paths = paths;
    }


    @Bean
    public int VaultDataImport() {
        if (!enabled) {
            logger.info("Vault: Not enabled");
            return -1;
        }
        checkSettings();
        logger.info("Vault: Enabled at {}", uri);

        try {
            var vaultRestTemplate = createRestTemplate();
            String token = getToken(vaultRestTemplate);

            paths.getPaths().forEach(
                    (mountName, children) -> {
                        try {
                            logger.info("Vault: Mount name \"{}\"", mountName);
                            traverse(vaultRestTemplate, token, mountName, (LinkedHashMap<?, ?>) children, "");
                        } catch (Exception e) {
                            throw new RuntimeException("Vault: Error for mount \"" + mountName + "\"", e);
                        }
                    }
            );
            logger.info("Vault: Data loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    public void traverse(RestTemplate vaultTemplate, String token, String mountName, LinkedHashMap<?, ?> map, String fullPath) {
        LinkedHashMap<?, ?> secretData = null;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();

            if (value instanceof LinkedHashMap) {
                traverse(vaultTemplate, token, mountName, (LinkedHashMap<?, ?>) value, fullPath += "/" + key);
            } else if (value instanceof String) { // Обработка других типов Map
                if (secretData == null) {
                    secretData = getData(vaultTemplate, token, mountName, fullPath);
                }
                applyData(secretData, fullPath, key, (String) value);
            }
        }
    }

    private void applyData(LinkedHashMap<?, ?> data, String fullPath, String vaultValueKey, String settingName) {
        String value = (String) data.get(vaultValueKey);
        if (value == null) {
            throw new RuntimeException("Vault: Not found KV \"" + fullPath + "/" + vaultValueKey + "\"");
        }
        logger.info("Vault: Found \"{}\"", fullPath + "/" + vaultValueKey);
        System.setProperty(settingName, value);
    }

    private void checkSettings() {
        if (uri == null || uri.isEmpty()) throw new RuntimeException("Vault \"uri\" not specified");

        if (sslType == null || sslType.isEmpty())
            throw new RuntimeException("Vault: \"ssl.type\" not specified");

        if (!sslType.equalsIgnoreCase("store") && !sslType.equalsIgnoreCase("cert"))
            throw new RuntimeException("Vault: \"ssl.type\" should be \"store\" or \"cert\"");

        if (sslType.equalsIgnoreCase("store")) {
            if (sslKeyStore == null || sslKeyStore.isEmpty())
                throw new RuntimeException("Vault: \"ssl.key-store\" not specified");
            if (sslKeyStoreType == null || sslKeyStoreType.isEmpty())
                throw new RuntimeException("Vault: \"ssl.key-store-type\" not specified");
            if (sslKeyStorePassword == null || sslKeyStorePassword.isEmpty())
                throw new RuntimeException("Vault: \"ssl.key-store-password\" not specified");
        } else {
            if (sslCertificate == null || sslCertificate.isEmpty())
                throw new RuntimeException("Vault: \"ssl.certificate\" not specified");
            if (sslKeyPath == null || sslKeyPath.isEmpty())
                throw new RuntimeException("Vault: \"ssl.key\" not specified");
        }
        if (paths == null || paths.isEmpty()) throw new RuntimeException("Vault \"paths\" not specified");
    }

    RestTemplate createRestTemplate() {
        return new RestTemplate(sslType.equalsIgnoreCase("store")
                ? new ClientHttpRequestFactoryPkcs(sslKeyStore, sslKeyStorePassword)
                : new ClientHttpRequestFactoryCrt(sslCertificate, sslKeyPath));
    }

    String getToken(RestTemplate restTemplate) {
        ResponseEntity<JSONObject> response =
                restTemplate.postForEntity(this.uri + "/v1/auth/cert/login", null, JSONObject.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Vault: Login failed with code " + response.getStatusCode().value());
        }
        JSONObject body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Vault: Login failed with empty body");
        }
        Object oauth = body.get("auth");
        if (!(oauth instanceof LinkedHashMap<?, ?> auth)) {
            throw new RuntimeException("Vault: Login failed with wrong body");
        }
        String token = (String) auth.get("client_token");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Vault: Login failed with empty token");
        }
        return token;
    }

    LinkedHashMap<?, ?> getData(RestTemplate restTemplate, String token, String mountName, String path) {
        String secretUri = uri + "/v1/" + mountName + "/data/" + path;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Vault-Token", token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            var response =
                    restTemplate.exchange(secretUri, HttpMethod.GET, requestEntity, JSONObject.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Vault: Data failed with code " + response.getStatusCode().value());
            }
            JSONObject body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Vault: Data failed with empty body");
            }
            LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) body.get("data");
            if (data == null) {
                throw new RuntimeException("Vault: Empty \"data\" object in response");
            }
            data = (LinkedHashMap<?, ?>) data.get("data");
            if (data == null) {
                throw new RuntimeException("Vault: Empty \"data.data\" object in response");
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Vault: \""+secretUri+"\" failed with exception", e);
        }

    }
}
