package com.olva.util;

import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class PemUtils {

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
    private static final String BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String END_RSA_PRIVATE_KEY = "-----END RSA PRIVATE KEY-----";

    private PemUtils() {
    }

    public static PrivateKey readPrivateKey(String path) {
        try {
            String pem = Files.readString(Path.of(path));
            byte[] decoded = decodePem(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(toPkcs8IfNeeded(pem, decoded));

            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Error leyendo llave privada PEM: " + path, e);
        }
    }

    private static byte[] decodePem(String pem) {
        String content = pem
                .replace(BEGIN_PRIVATE_KEY, "")
                .replace(END_PRIVATE_KEY, "")
                .replace(BEGIN_RSA_PRIVATE_KEY, "")
                .replace(END_RSA_PRIVATE_KEY, "")
                .replaceAll("\\s+", "");

        return Base64.getDecoder().decode(content);
    }

    private static byte[] toPkcs8IfNeeded(String pem, byte[] decoded) throws Exception {
        if (!pem.contains(BEGIN_RSA_PRIVATE_KEY)) {
            return decoded;
        }

        RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(decoded);
        PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(
                new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
                rsaPrivateKey
        );

        return privateKeyInfo.getEncoded();
    }
}
