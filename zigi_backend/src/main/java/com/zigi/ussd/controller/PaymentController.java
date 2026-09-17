package com.zigi.ussd.controller;

import com.zigi.ussd.model.Transaction;
import com.zigi.ussd.repository.TransactionRepository;
import com.zigi.ussd.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;

    public PaymentController(PaymentService paymentService, TransactionRepository transactionRepository) {
        this.paymentService = paymentService;
        this.transactionRepository = transactionRepository;
    }

    public static class InitiateRequest {
        public String phoneNumber;
        public Long menuItemId;
        public String provider; 
    }

    @PostMapping
    public ResponseEntity<?> initiate(@RequestBody InitiateRequest req,
                                       @RequestHeader(value = "X-Client-Channel", required = false) String channel) {
        if (req.phoneNumber == null || req.phoneNumber.isBlank() || req.menuItemId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "phoneNumber and menuItemId are required."));
        }
        PaymentService.InitiateResult result = paymentService.initiatePayment(req.phoneNumber, req.menuItemId, req.provider, channel);
        if (result.error != null) {
            return ResponseEntity.badRequest().body(Map.of("message", result.error));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.transaction);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getStatus(@PathVariable Long id) {
        Optional<Transaction> txn = transactionRepository.findById(id);
        return txn.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam Long transactionId,
            @RequestParam String externalRef,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String providerMessage) {

        Optional<Transaction> updated = paymentService.applyCallback(transactionId, externalRef, status, providerMessage);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Transaction " + transactionId + " updated to " + status));
    }
}
