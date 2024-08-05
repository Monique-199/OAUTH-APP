// File: src/main/java/com/example/MpesaApis/MpesaService.java
package com.example.MpesaApis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
                return rootNode.path("access_token").asText();
            } else {
                throw new RuntimeException("Failed to retrieve access token: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token retrieval failed: " + e.getMessage(), e);
        }
    }

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
