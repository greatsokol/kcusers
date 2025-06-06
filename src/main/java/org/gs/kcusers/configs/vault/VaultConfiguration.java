package org.gs.kcusers.configs.vault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientCertificateAuthentication;
import org.springframework.vault.authentication.ClientCertificateAuthenticationOptions;
import org.springframework.vault.client.RestTemplateBuilder;
import org.springframework.vault.client.SimpleVaultEndpointProvider;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
public class VaultConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(VaultConfiguration.class);

    @Value("${vault.enabled:false}")
    boolean enabled;

    @Value("${vault.host:#{null}}")
    String host;

    @Value("${vault.port:-1}")
    int port;

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
    VaultConfiguration(VaultPaths paths) {
        this.paths = paths;
    }

    @Bean
    public int VaultDataImport() {
        if (!enabled) {
            logger.info("Vault: Not enabled");
            return -1;
        }
        checkSettings();
        logger.info("Vault: Enabled at {}:{}", host, port);

        try {
            var vaultEndpoint = VaultEndpoint.create(host, port);
            var vaultRestTemplate = RestTemplateBuilder
                    .builder()
                    .requestFactory(
                            sslType.equalsIgnoreCase("store")
                                    ? new VaultClientHttpRequestFactoryPkcs(sslKeyStore, sslKeyStorePassword)
                                    : new VaultClientHttpRequestFactoryCrt(sslCertificate, sslKeyPath)
                    )
                    .endpointProvider(SimpleVaultEndpointProvider.of(vaultEndpoint)).build();
            var vaultOptions = ClientCertificateAuthenticationOptions.builder().build();
            var auth = new ClientCertificateAuthentication(vaultOptions, vaultRestTemplate);
            var vaultTemplate = new VaultTemplate(vaultEndpoint, auth);

            paths.getPaths().forEach(
                    (engineName, children) -> {
                        logger.info("Vault: Engine name \"{}\"", engineName);
                        if (children instanceof LinkedHashMap)
                            traverse(vaultTemplate, engineName, (LinkedHashMap<String, Object>) children, "");
                    }
            );
            logger.info("Vault: Data loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    public void traverse(VaultTemplate vaultTemplate, String engineName, LinkedHashMap<String, Object> map, String fullPath) {
        Map<String, Object> secretData = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof LinkedHashMap) {
                traverse(vaultTemplate, engineName, (LinkedHashMap<String, Object>) value, fullPath += "/" + key);
            } else if (value instanceof String) { // Обработка других типов Map
                if(secretData == null) {
                    secretData = getDataFromSecret(vaultTemplate, engineName, fullPath);
                }
                applySettings(secretData, fullPath, key, (String) value);
            }
        }
    }


    private Map<String, Object> getDataFromSecret(VaultTemplate vaultTemplate, String engineName, String secretName) {
        var vaultKeyValueOperations
                = vaultTemplate.opsForKeyValue(engineName, VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
        logger.info("Vault: Getting secret \"{}\" from \"{}\" engine", secretName, engineName);
        var valuesKv = vaultKeyValueOperations.get(secretName);
        if(valuesKv == null) {
            throw new RuntimeException("Vault: Not found secret \"" + secretName + "\"");
        }
        var data = valuesKv.getData();
        if(data == null) {
            throw new RuntimeException("Vault: No data in secret \"" + secretName + "\"");
        }
        return data;
    }

    private void applySettings(Map<String, Object> data, String fullPath, String vaultValueKey, String settingName) {
        String value = (String) data.get(vaultValueKey);
        if(value == null) {
            throw new RuntimeException("Vault: Not found KV \"" + fullPath+"/"+vaultValueKey + "\"");
        }
        logger.info("Vault: Found \"{}\"", fullPath+"/"+vaultValueKey);
        System.setProperty(settingName, value);
    }

    private void checkSettings() {
        if (host == null || host.isEmpty()) throw new RuntimeException("Vault \"host\" not specified");
        if (port < 0) throw new RuntimeException("Vault: \"port\" not specified");

        if (sslType == null || sslType.isEmpty())
            throw new RuntimeException("Vault: \"ssl.type\" not specified");

        if (!sslType.equalsIgnoreCase("store") && !sslType.equalsIgnoreCase("cert"))
            throw new RuntimeException("Vault: \"ssl.type\" should be \"store\" or \"cert\"");

        if (sslType.equalsIgnoreCase("store")) {
            if (sslKeyStore == null || sslKeyStore.isEmpty())
                throw new RuntimeException("Vault \"ssl.key-store\" not specified");
            if (sslKeyStoreType == null || sslKeyStoreType.isEmpty())
                throw new RuntimeException("Vault \"ssl.key-store-type\" not specified");
            if (sslKeyStorePassword == null || sslKeyStorePassword.isEmpty())
                throw new RuntimeException("Vault \"ssl.key-store-password\" not specified");
        } else {
            if (sslCertificate == null || sslCertificate.isEmpty())
                throw new RuntimeException("Vault \"ssl.certificate\" not specified");
            if (sslKeyPath == null || sslKeyPath.isEmpty())
                throw new RuntimeException("Vault \"ssl.key\" not specified");
        }
        if (paths == null || paths.isEmpty()) throw new RuntimeException("Vault \"engines\" not specified");
    }
}
