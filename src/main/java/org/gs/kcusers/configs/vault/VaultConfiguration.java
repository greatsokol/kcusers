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

    private final VaultEngines engines;

    @Autowired
    VaultConfiguration(VaultEngines engines) {
        this.engines = engines;
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

            engines.getEngines().forEach(
                    (engineName, engine) -> {
                        logger.info("Vault: looking for secrets in \"{}\" engine", engineName);
                        engine.forEach((secretName, valuesDestinations) -> {
                            var data = getDataFromSecret(vaultTemplate, engineName, secretName);
                            valuesDestinations.forEach((vaultValueKey, settingName) ->
                                    applySettings(data, vaultValueKey, settingName)
                            );
                        });
                    }
            );
            logger.info("Vault: Data loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private Map<String, Object> getDataFromSecret(VaultTemplate vaultTemplate, String engineName, String secretName) {
        var vaultKeyValueOperations
                = vaultTemplate.opsForKeyValue(engineName, VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
        logger.info("Vault: Looking for values in \"{}\" secret", secretName);
        var valuesKv = Objects.requireNonNull(vaultKeyValueOperations.get(secretName));
        return Objects.requireNonNull(valuesKv.getData());
    }

    private void applySettings(Map<String, Object> data, String vaultValueKey, String settingName) {
        String value = (String) Objects.requireNonNull(data.get(vaultValueKey));
        logger.info("Vault: Loaded \"{}\" from Vault", vaultValueKey);
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
        if (engines == null || engines.isEmpty()) throw new RuntimeException("Vault \"engines\" not specified");
    }
}
