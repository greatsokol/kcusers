package org.gs.kcusers.configs.vault;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.FileInputStream;
import java.security.KeyStore;

import static org.springframework.util.ResourceUtils.getFile;
import static org.springframework.util.ResourceUtils.isUrl;

public class ClientHttpRequestFactoryPkcs extends HttpComponentsClientHttpRequestFactory {
    public ClientHttpRequestFactoryPkcs(
            String keyStoreLocation,
            String keyStorePassword
    ) {
        try {
            char[] pKeyStorePassword = keyStorePassword.toCharArray();

            var keyStore = KeyStore.getInstance("pkcs12");
            if (isUrl(keyStoreLocation)) {
                keyStoreLocation = getFile(keyStoreLocation).getPath();
            }
            keyStore.load(new FileInputStream(keyStoreLocation), pKeyStorePassword);

            var sslContext = new SSLContextBuilder().loadKeyMaterial(keyStore,
                    pKeyStorePassword).loadTrustMaterial((cert, url) -> true).build();
            var sslConFactory = new SSLConnectionSocketFactory(sslContext);
//            var sslConFactory = SSLConnectionSocketFactoryBuilder.create()
//                    .setSslContext(sslContext)
//                    .setTlsVersions(TLS.V_1_2, TLS.V_1_3)
//                    .build();
            var cm = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslConFactory).build();
            var httpClient = HttpClients.custom().setConnectionManager(cm).build();
            setHttpClient(httpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
