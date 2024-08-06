// File: src/main/java/com/example/MpesaApis/controllers/MpesaController.java
package com.example.MpesaApis.controllers;

import com.example.MpesaApis.MpesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mpesaapis")  // Common prefix for all routes
public class MpesaController {

    @Autowired
    private MpesaService mpesaService;

    @GetMapping("/getToken")
    public String getToken() {
        return mpesaService.getAccessToken();
    }

    @PostMapping("/registerUrls")
    public String registerUrls() {
        try {
            String accessToken = mpesaService.getAccessToken();
            return mpesaService.registerUrls(accessToken);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    @PostMapping("/simulateC2BPayment")
    public String simulateC2BPayment(@RequestParam String phoneNumber, @RequestParam String amount, @RequestParam String accountReference) {
        try {
            String accessToken = mpesaService.getAccessToken();
            return mpesaService.simulateC2BPayment(accessToken, phoneNumber, amount, accountReference);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    @PostMapping("/b2cPayment")
    public String initiateB2CPayment(@RequestParam String phoneNumber, @RequestParam String amount, @RequestParam String commandID, @RequestParam String remarks, @RequestParam String occasion) {
        try {
            String accessToken = mpesaService.getAccessToken();
            return mpesaService.initiateB2CPayment(accessToken, phoneNumber, amount, commandID, remarks, occasion);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    @PostMapping("/stkPush")
    public String performStkPush(@RequestParam String phoneNumber, @RequestParam String amount) {
        try {
            String accessToken = mpesaService.getAccessToken();
            return mpesaService.performStkPush(accessToken, phoneNumber, amount);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody String payload) {
        // Process the callback payload
        System.out.println("Received callback: " + payload);
        // Here you can parse and store the callback information as needed
        return ResponseEntity.ok("Callback received");
    }

    @PostMapping("/validation")
    public ResponseEntity<String> handleValidation(@RequestBody String payload) {
        // Process the validation payload
        System.out.println("Received validation: " + payload);
        // Here you can parse and store the validation information as needed
        return ResponseEntity.ok("Validation received");
    }

    @PostMapping("/confirmation")
    public ResponseEntity<String> handleConfirmation(@RequestBody String payload) {
        // Process the confirmation payload
        System.out.println("Received confirmation: " + payload);
        // Here you can parse and store the confirmation information as needed
        return ResponseEntity.ok("Confirmation received");
    }
}
