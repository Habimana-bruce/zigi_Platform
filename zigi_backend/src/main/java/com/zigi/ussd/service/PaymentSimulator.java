package com.zigi.ussd.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Component
public class PaymentSimulator {

    private static final String CALLBACK_URL = "http://localhost:6157/api/payments/callback";

    public void simulateProviderCall(Long transactionId, String phoneNumber, BigDecimal amount) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2500); 

                boolean shouldFail = phoneNumber != null && phoneNumber.endsWith("0000");
                String status = shouldFail ? "FAILED" : "SUCCESS";
                String externalRef = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String message = shouldFail
                        ? "Subscriber declined or insufficient funds (simulated)."
                        : "Payment approved by subscriber (simulated).";

                String body = String.format(
                        "transactionId=%d&externalRef=%s&status=%s&providerMessage=%s",
                        transactionId, externalRef, status, message.replace(" ", "+"));

                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(CALLBACK_URL))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                client.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                
                System.err.println("PaymentSimulator callback failed: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
