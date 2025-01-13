package com.example.MpesaApis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
public class MpesaTransactionStatusService {

    @Value("${MpesaApis.transanction.status.url}")
    private String transactionStatusUrl;

    @Value("${MpesaApis.consumer-key}")
    private String consumerKey;

    @Value("${MpesaApis.consumer-secret}")
    private String consumerSecret;

    @Value("${MpesaApis.authorization.url}")
    private String apiUrl;

    @Value("${MpesaApis.shortcode}")
    private String shortcode;

    @Value("${MpesaApis.initiator.password}")
    private String initiatorPassword;

    @Value("${MpesaApis.certificate.path}")
    private String certificatePath;

    @Value("${MpesaApis.validation.url}")
    private String validationUrl;

    @Value("${MpesaApis.confirmation.url}")
    private String confirmationUrl;

    public String getAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        String credentials = consumerKey + ":" + consumerSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedCredentials);
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());
                System.out.println("Access Token Response: " + response.getBody());
                return rootNode.path("access_token").asText();
            } else {
                throw new RuntimeException("Failed to retrieve access token: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token retrieval failed: " + e.getMessage(), e);
        }
    }

    public String checkTransactionStatus(String accessToken, String transactionID, String remarks) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"Initiator\":\"" + shortcode + "\","
                    + "\"SecurityCredential\":\"" + generateSecurityCredential() + "\","
                    + "\"CommandID\":\"TransactionStatusQuery\","
                    + "\"TransactionID\":\"" + transactionID + "\","
                    + "\"PartyA\":\"" + shortcode + "\","
                    + "\"IdentifierType\":\"1\","
                    + "\"Remarks\":\"" + remarks + "\","
                    + "\"QueueTimeOutURL\":\"" + validationUrl + "\","
                    + "\"ResultURL\":\"" + confirmationUrl + "\""
                    + "}";

            // Log the request payload and headers
            System.out.println("Transaction Status Request Payload: " + requestBody);
            System.out.println("Authorization Header: Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(transactionStatusUrl, HttpMethod.POST, entity, String.class);

            // Log the response status and body
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to check transaction status: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to check transaction status: " + e.getMessage(), e);
        }
    }

    private PublicKey getPublicKey(String certificatePath) throws Exception {
        System.out.println("Attempting to load public key from certificate: " + certificatePath);
        File file = new File(certificatePath);
        if (!file.exists()) {
            throw new RuntimeException("Certificate file not found: " + certificatePath);
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fis);
            System.out.println("Successfully loaded public key from certificate");
            return certificate.getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from certificate: " + e.getMessage(), e);
        }
    }

    private String encryptInitiatorPassword(String password, PublicKey publicKey) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt initiator password: " + e.getMessage(), e);
        }
    }

    private String generateSecurityCredential() {
        try {
            System.out.println("Loading public key from certificate: " + certificatePath);
            PublicKey publicKey = getPublicKey(certificatePath);
            System.out.println("Encrypting initiator password");
            return encryptInitiatorPassword(initiatorPassword, publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate security credential: " + e.getMessage(), e);
        }
    }
}