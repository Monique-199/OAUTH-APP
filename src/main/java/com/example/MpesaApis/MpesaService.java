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

    // M-Pesa API URLs and credentials fetched from application properties
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

    @Value("${MpesaApis.initiator.password}")
    private String initiatorPassword;

    // Method to register M-Pesa URLs with the API
    public String registerUrls(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Bearer token for authorization
            headers.set("Content-Type", "application/json");

            // Construct the request body for registering URLs
            String requestBody = "{"
                    + "\"ShortCode\":\"" + shortcode + "\","
                    + "\"ResponseType\":\"Completed\","
                    + "\"ConfirmationURL\":\"" + confirmationUrl + "\","
                    + "\"ValidationURL\":\"" + validationUrl + "\""
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Send POST request to register URLs
            ResponseEntity<String> response = restTemplate.exchange(registerUrl, HttpMethod.POST, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to register URLs: " + e.getMessage());
        }
    }

    // Method to simulate a C2B (Customer to Business) payment
    public String simulateC2BPayment(String accessToken, String phoneNumber, String amount, String accountReference) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Bearer token for authorization
            headers.set("Content-Type", "application/json");

            // Construct the request body for simulating a C2B payment
            String requestBody = "{"
                    + "\"ShortCode\":\"" + shortcode + "\","
                    + "\"CommandID\":\"CustomerPayBillOnline\","
                    + "\"Amount\":\"" + amount + "\","
                    + "\"Msisdn\":\"" + phoneNumber + "\","
                    + "\"BillRefNumber\":\"" + accountReference + "\""
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Send POST request to simulate C2B payment
            ResponseEntity<String> response = restTemplate.exchange(c2bUrl, HttpMethod.POST, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to simulate C2B payment: " + e.getMessage());
        }
    }

    // Method to perform STK (Simulated Till/Paybill) push for payments
    public String performStkPush(String accessToken, String phoneNumber, String amount) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Bearer token for authorization
            headers.set("Content-Type", "application/json");

            // Construct the request body for the STK push
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

            // Send POST request to perform STK push
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

    // Method to get an access token using consumer key and secret
    public String getAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        String credentials = consumerKey + ":" + consumerSecret;  // Combine credentials for Basic Auth
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());  // Base64 encode credentials

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedCredentials);  // Add Basic Authorization header
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Send GET request to retrieve access token
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());
                return rootNode.path("access_token").asText();  // Extract access token from response
            } else {
                throw new RuntimeException("Failed to retrieve access token: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token retrieval failed: " + e.getMessage(), e);
        }
    }

    // Method to initiate a B2C (Business to Customer) payment
    public String initiateB2CPayment(String accessToken, String phoneNumber, String amount, String commandID, String remarks, String occasion) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Bearer token for authorization
            headers.set("Content-Type", "application/json");

            // Construct the request body for initiating B2C payment
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

            // Send POST request to initiate B2C payment
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
            throw new RuntimeException("Failed to initiate B2C payment: " + e.getMessage());
        }
    }

    // Utility method to generate password for STK push request (based on your API logic)
    private String generatePassword() {
        // Example: generate a password based on specific logic
        return "your_password";
    }

    // Utility method to generate security credential for B2C payment
    private String generateSecurityCredential() {
        return "your_security_credential";
    }

    // Utility method to get the current timestamp formatted as required by the M-Pesa API
    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }
}
