// File: src/main/java/com/example/MpesaApis/MpesaService.java
package com.example.MpesaApis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Service
public class MpesaService {

    @Value("${MpesaApis.authorization.url}")
    private String apiUrl;

    @Value("${MpesaApis.consumer-key}")
    private String consumerKey;

    @Value("${MpesaApis.consumer-secret}")
    private String consumerSecret;

    @Value("${MpesaApis.register.url}")
    private String registerUrl;

    @Value("${MpesaApis.c2b.url}")
    private String c2bUrl;

    @Value("${MpesaApis.stk.push.url}")
    private String stkPushUrl;

    @Value("${MpesaApis.shortcode}")
    private String shortcode;

    @Value("${MpesaApis.passkey}")
    private String passkey;

    @Value("${MpesaApis.validation.url}")
    private String validationUrl;

    @Value("${MpesaApis.confirmation.url}")
    private String confirmationUrl;

    @Value("${MpesaApis.b2c.url}")
    private String b2cUrl;  // Add the B2C URL

    @Value("${MpesaApis.certificate.path}")
    private String certificatePath;



    @Value("{MpesaApis.initiator.password}")
    private String initiatorPassword;

//GENERATE ACCESS TOKEN

 //REGISTER URLS
    public String registerUrls(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"ShortCode\":\"" + shortcode + "\","
                    + "\"ResponseType\":\"Completed\","
                    + "\"ConfirmationURL\":\"" + confirmationUrl + "\","
                    + "\"ValidationURL\":\"" + validationUrl + "\""
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(registerUrl, HttpMethod.POST, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to register URLs: " + e.getMessage());
        }
    }
    //C2B METHOD

    public String simulateC2BPayment(String accessToken, String phoneNumber, String amount, String accountReference) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"ShortCode\":\"" + shortcode + "\","
                    + "\"CommandID\":\"CustomerPayBillOnline\","
                    + "\"Amount\":\"" + amount + "\","
                    + "\"Msisdn\":\"" + phoneNumber + "\","
                    + "\"BillRefNumber\":\"" + accountReference + "\""
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(c2bUrl, HttpMethod.POST, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to simulate C2B payment: " + e.getMessage());
        }
    }
//STK PUSH METHOD
    public String performStkPush(String accessToken, String phoneNumber, String amount) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"BusinessShortCode\":\"" + shortcode + "\","
                    + "\"Password\":\"" + generatePassword() + "\","
                    + "\"Timestamp\":\"" + getTimestamp() + "\","
                    + "\"TransactionType\":\"CustomerPayBillOnline\","
                    + "\"Amount\":\"" + amount + "\","
                    + "\"PartyA\":\"" + phoneNumber + "\","
                    + "\"PartyB\":\"" + shortcode + "\","
                    + "\"PhoneNumber\":\"" + phoneNumber + "\","
                    + "\"CallBackURL\":\"https://httpbin.org/post\","
                    + "\"AccountReference\":\"" + phoneNumber + "\","
                    + "\"TransactionDesc\":\"Payment for testing\""
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(stkPushUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to perform STK push: " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to perform STK push: " + e.getMessage());
        }
    }

    //B2C METHOD
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
                System.out.println("Access Token Response: " + response.getBody());  // Log the response
                return rootNode.path("access_token").asText();
            } else {
                throw new RuntimeException("Failed to retrieve access token: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token retrieval failed: " + e.getMessage(), e);
        }
    }

    public String initiateB2CPayment(String accessToken, String phoneNumber, String amount, String commandID, String remarks, String occasion) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"InitiatorName\":\"" + shortcode + "\","
                    + "\"SecurityCredential\":\"" + generateSecurityCredential() + "\","
                    + "\"CommandID\":\"" + commandID + "\","
                    + "\"Amount\":\"" + amount + "\","
                    + "\"PartyA\":\"" + shortcode + "\","
                    + "\"PartyB\":\"" + phoneNumber + "\","
                    + "\"Remarks\":\"" + remarks + "\","
                    + "\"QueueTimeOutURL\":\"" + validationUrl + "\","
                    + "\"ResultURL\":\"" + confirmationUrl + "\","
                    + "\"Occasion\":\"" + occasion + "\""
                    + "}";

            // Log the request payload and headers
            System.out.println("B2C Payment Request Payload: " + requestBody);
            System.out.println("Authorization Header: Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(b2cUrl, HttpMethod.POST, entity, String.class);

            // Log the response
            System.out.println("B2C Payment Response: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to initiate B2C payment: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initiate B2C payment: " + e.getMessage(), e);
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
    private String generatePassword() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String password = shortcode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(password.getBytes());
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }


}
