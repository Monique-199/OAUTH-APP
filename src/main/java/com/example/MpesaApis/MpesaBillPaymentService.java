package com.example.MpesaApis;

import com.example.MpesaApis.util.MpesaSecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.PublicKey;
import java.util.Base64;

@Service
public class MpesaBillPaymentService {

    @Value("${MpesaApis.authorization.url}")
    private String apiUrl;

    @Value("${MpesaApis.consumer-key}")
    private String consumerKey;

    @Value("${MpesaApis.consumer-secret}")
    private String consumerSecret;

    @Value("${MpesaApis.billpayment.url}")
    private String billPaymentUrl;

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
                return rootNode.path("access_token").asText();
            } else {
                throw new RuntimeException("Failed to retrieve access token: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Token retrieval failed: " + e.getMessage(), e);
        }
    }

    public String payBill(String accessToken, String amount, String accountReference, String partyA, String partyB) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/json");

            String requestBody = "{"
                    + "\"Initiator\":\"" + shortcode + "\","
                    + "\"SecurityCredential\":\"" + generateSecurityCredential() + "\","
                    + "\"CommandID\":\"BusinessPayment\","
                    + "\"Amount\":\"" + amount + "\","
                    + "\"PartyA\":\"" + partyA + "\","
                    + "\"PartyB\":\"" + partyB + "\","
                    + "\"ReceiverIdentifierType\":\"1\","
                    + "\"Remarks\":\"Bill Payment\","  // Added Remarks
                    + "\"AccountReference\":\"" + accountReference + "\","
                    + "\"QueueTimeOutURL\":\"" + validationUrl + "\","
                    + "\"ResultURL\":\"" + confirmationUrl + "\""
                    + "}";

            System.out.println("Request URL: " + billPaymentUrl);
            System.out.println("Request Headers: " + headers);
            System.out.println("Request Body: " + requestBody);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(billPaymentUrl, HttpMethod.POST, entity, String.class);

            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Headers: " + response.getHeaders());
            System.out.println("Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseBillPaymentResponse(response.getBody());
            } else {
                throw new RuntimeException("Failed to perform bill payment: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to perform bill payment: " + e.getMessage(), e);
        }
    }

    private String parseBillPaymentResponse(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);

            JsonNode resultNode = rootNode.path("Result");

            String resultType = resultNode.path("ResultType").asText("");
            String resultCode = resultNode.path("ResultCode").asText("");
            String resultDesc = resultNode.path("ResultDesc").asText("");
            String originatorConversationID = resultNode.path("OriginatorConversationID").asText("");
            String conversationID = resultNode.path("ConversationID").asText("");
            String transactionID = resultNode.path("TransactionID").asText("");

            return String.format(
                    "ResultType: %s\nResultCode: %s\nResultDesc: %s\nOriginatorConversationID: %s\nConversationID: %s\nTransactionID: %s",
                    resultType, resultCode, resultDesc, originatorConversationID, conversationID, transactionID
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse bill payment response: " + e.getMessage(), e);
        }
    }

    private String generateSecurityCredential() {
        try {
            PublicKey publicKey = MpesaSecurityUtils.getPublicKey(certificatePath);
            return MpesaSecurityUtils.generateSecurityCredential(initiatorPassword, publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate security credential: " + e.getMessage(), e);
        }
    }
}
