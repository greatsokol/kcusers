package org.gs.kcusers.configs.vault;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.springframework.util.ResourceUtils.getFile;
import static org.springframework.util.ResourceUtils.isUrl;

public class VaultClientHttpRequestFactoryCrt extends HttpComponentsClientHttpRequestFactory {
    private static final String keyStorePsw = "";

    public VaultClientHttpRequestFactoryCrt(
            String certificateFileName,
            String keyFileName
    ) {
        try {
            var keyStore = loadKeyStoreFromCrtAndKey(certificateFileName, keyFileName);

            var sslContext = new SSLContextBuilder().loadKeyMaterial(keyStore,
                    keyStorePsw.toCharArray()).loadTrustMaterial((cert, url) -> true).build();
            var sslConFactory = new SSLConnectionSocketFactory(sslContext);
            var cm = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslConFactory).build();
            var httpClient = HttpClients.custom().setConnectionManager(cm).build();
            setHttpClient(httpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyStore loadKeyStoreFromCrtAndKey(String certificateFileName, String keyFileName)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);  // Initialize an empty keystore

        try (var certInput = getFileFromResource(certificateFileName);
             var keyInput = getFileFromResource(keyFileName)
        ) {

            var certFactory = CertificateFactory.getInstance("X.509");
            var cert = (X509Certificate) certFactory.generateCertificate(certInput);

            // Load private key from PEM file
            var privateKey = readPrivateKeyFromKeyFile(keyInput);

            // Load certificate into the keystore
            keyStore.setCertificateEntry("public-key", cert);
            //keyStore.setCertificateEntry("cacert", ca);

            // Load private key into the keystore
            keyStore.setKeyEntry("private-key", privateKey, keyStorePsw.toCharArray(),
                    new java.security.cert.Certificate[]{cert /*, ca*/});
        }

        return keyStore;
    }

    private static FileInputStream getFileFromResource(String fileName) throws FileNotFoundException {
        if (isUrl(fileName)) {
            fileName = getFile(fileName).getPath();
        }
        var file = new File(fileName);
        return new FileInputStream(file);
    }

    private static PrivateKey readPrivateKeyFromKeyFile(InputStream keyInput) throws IOException {
        try (var pemParser = new PEMParser(new InputStreamReader(keyInput))) {
            var converter = new JcaPEMKeyConverter();
            Object keyObject = pemParser.readObject();
            if (keyObject instanceof PEMKeyPair) {
                return converter.getPrivateKey(((PEMKeyPair) keyObject).getPrivateKeyInfo());
            } else if (keyObject instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) keyObject);
            } else {
                throw new IOException("Invalid private key format in key file");
            }
        }
    }
}
