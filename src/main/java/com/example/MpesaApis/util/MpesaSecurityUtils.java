package com.example.MpesaApis.util;


import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class MpesaSecurityUtils {

    public static String generateSecurityCredential(String initiatorPassword, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(initiatorPassword.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate security credential: " + e.getMessage(), e);
        }
    }

    public static PublicKey getPublicKey(String certificatePath) throws Exception {
        File file = new File(certificatePath);
        if (!file.exists()) {
            throw new RuntimeException("Certificate file not found: " + certificatePath);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fis);
            return certificate.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from certificate: " + e.getMessage(), e);
        }
    }
}
