// File: src/main/java/com/example/MpesaApis/MpesaReversalService.java
package com.example.MpesaApis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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
public class MpesaReversalService {

    @Value("${MpesaApis.authorization.url}")
    private String apiUrl;

    @Value("${MpesaApis.consumer-key}")
    private String consumerKey;

    @Value("${MpesaApis.consumer-secret}")
    private String consumerSecret;

    @Value("${MpesaApis.reversal.url}")
    private String reversalUrl;

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

    public String performReversal(String accessToken, String transactionID, String amount, String receiverParty, String remarks) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"Initiator\":\"" + shortcode + "\","
                    + "\"SecurityCredential\":\"" + generateSecurityCredential() + "\","
                    + "\"CommandID\":\"TransactionReversal\","
                    + "\"TransactionID\":\"" + transactionID + "\","
                    + "\"Amount\":\"" + amount + "\","
                    + "\"ReceiverParty\":\"" + receiverParty + "\","
                    + "\"ReceiverIdentifierType\":\"11\","
                    + "\"Remarks\":\"" + remarks + "\","
                    + "\"QueueTimeOutURL\":\"" + validationUrl + "\","
                    + "\"ResultURL\":\"" + confirmationUrl + "\""
                    + "}";

            // Log the request payload and headers
            System.out.println("Reversal Request Payload: " + requestBody);
            System.out.println("Authorization Header: Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(reversalUrl, HttpMethod.POST, entity, String.class);

            // Log the response status and body
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseReversalResponse(response.getBody());
            } else {
                throw new RuntimeException("Failed to perform reversal: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to perform reversal: " + e.getMessage(), e);
        }
    }

    private String parseReversalResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            System.out.println("Parsed JSON Response: " + rootNode.toPrettyString());

            JsonNode resultNode = rootNode.path("Result");

            String resultType = resultNode.path("ResultType").asText("");
            String resultCode = resultNode.path("ResultCode").asText("");
            String resultDesc = resultNode.path("ResultDesc").asText("");
            String originatorConversationID = resultNode.path("OriginatorConversationID").asText("");
            String conversationID = resultNode.path("ConversationID").asText("");
            String transactionID = resultNode.path("TransactionID").asText("");

            StringBuilder resultDetails = new StringBuilder();
            JsonNode resultParametersNode = resultNode.path("ResultParameters");
            if (resultParametersNode.isArray()) {
                for (JsonNode parameterNode : resultParametersNode) {
                    String key = parameterNode.path("Key").asText("");
                    String value = parameterNode.path("Value").asText("");
                    resultDetails.append(key).append(": ").append(value).append("\n");
                }
            }

            return String.format(
                    "ResultType: %s\nResultCode: %s\nResultDesc: %s\nOriginatorConversationID: %s\nConversationID: %s\nTransactionID: %s\n%s",
                    resultType, resultCode, resultDesc, originatorConversationID, conversationID, transactionID, resultDetails.toString()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse reversal response: " + e.getMessage(), e);
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
