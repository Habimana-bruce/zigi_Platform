package com.zigi.ussd.service;

import com.zigi.ussd.model.*;
import com.zigi.ussd.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerRepository customerRepository;
    private final PaymentSimulator paymentSimulator;

    public PaymentService(TransactionRepository transactionRepository,
                           PaymentAttemptRepository attemptRepository,
                           MenuItemRepository menuItemRepository,
                           CustomerRepository customerRepository,
                           PaymentSimulator paymentSimulator) {
        this.transactionRepository = transactionRepository;
        this.attemptRepository = attemptRepository;
        this.menuItemRepository = menuItemRepository;
        this.customerRepository = customerRepository;
        this.paymentSimulator = paymentSimulator;
    }

    public static class InitiateResult {
        public Transaction transaction;
        public String error;
    }

    public InitiateResult initiatePayment(String phoneNumber, Long menuItemId, String provider, String channel) {
        InitiateResult result = new InitiateResult();

        Optional<MenuItem> itemOpt = menuItemRepository.findById(menuItemId);
        if (itemOpt.isEmpty() || Boolean.TRUE.equals(itemOpt.get().getIsDeleted())) {
            result.error = "Menu item " + menuItemId + " not found.";
            return result;
        }
        MenuItem item = itemOpt.get();
        if (!"PAYMENT".equals(item.getActionType()) || item.getPrice() == null) {
            result.error = "Menu item " + menuItemId + " (" + item.getLabel() + ") is not a payable item.";
            return result;
        }

        Transaction txn = new Transaction();
        txn.setPhoneNumber(phoneNumber);
        txn.setMenuItemId(item.getId());
        txn.setDescription(item.getLabel());
        txn.setAmount(item.getPrice());
        txn.setProvider(provider != null ? provider.toUpperCase() : "MTN");
        txn.setStatus("PENDING");
        txn.setChannel(channel != null ? channel : "UNKNOWN");
        txn.setCreatedAt(LocalDateTime.now());
        txn.setUpdatedAt(LocalDateTime.now());
        txn = transactionRepository.save(txn);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setTransactionId(txn.getId());
        attempt.setAttemptNumber(1);
        attempt.setProvider(txn.getProvider());
        attempt.setRequestPayload(String.format(
                "{\"phoneNumber\":\"%s\",\"amount\":%s,\"description\":\"%s\"}",
                phoneNumber, item.getPrice(), item.getLabel()));
        attempt.setStatus("PENDING");
        attempt.setCreatedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        paymentSimulator.simulateProviderCall(txn.getId(), phoneNumber, txn.getAmount());

        result.transaction = txn;
        return result;
    }

    public Optional<Transaction> applyCallback(Long transactionId, String externalRef, String status, String providerMessage) {
        Optional<Transaction> txnOpt = transactionRepository.findById(transactionId);
        if (txnOpt.isEmpty()) return Optional.empty();

        Transaction txn = txnOpt.get();
        txn.setStatus(status);
        txn.setExternalRef(externalRef);
        txn.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(txn);

        List<PaymentAttempt> attempts = attemptRepository.findByTransactionIdOrderByAttemptNumberDesc(transactionId);
        if (!attempts.isEmpty()) {
            PaymentAttempt latest = attempts.get(0);
            latest.setStatus(status);
            latest.setResponsePayload(String.format(
                    "{\"externalRef\":\"%s\",\"status\":\"%s\",\"message\":\"%s\"}",
                    externalRef, status, providerMessage));
            attemptRepository.save(latest);
        }

        if ("SUCCESS".equals(status)) {
            creditCustomer(txn);
        }

        return Optional.of(txn);
    }

    private void creditCustomer(Transaction txn) {
        Optional<MenuItem> itemOpt = menuItemRepository.findById(txn.getMenuItemId());
        if (itemOpt.isEmpty() || itemOpt.get().getCreditType() == null) return;
        MenuItem item = itemOpt.get();

        Customer customer = customerRepository.findByPhoneNumberAndIsDeletedFalse(txn.getPhoneNumber())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setFullName("Customer " + txn.getPhoneNumber());
                    c.setPhoneNumber(txn.getPhoneNumber());
                    c.setMainBalance(BigDecimal.ZERO);
                    c.setAirtimeBalance(BigDecimal.ZERO);
                    c.setDataBalanceMb(0);
                    c.setMinutesBalance(0);
                    c.setIsDeleted(false);
                    return c;
                });

        BigDecimal amount = item.getCreditAmount() != null ? item.getCreditAmount() : BigDecimal.ZERO;

        switch (item.getCreditType()) {
            case "DATA_MB":
                customer.setDataBalanceMb(customer.getDataBalanceMb() + amount.intValue());
                break;
            case "AIRTIME":
                customer.setAirtimeBalance(customer.getAirtimeBalance().add(amount));
                break;
            case "MINUTES":
                customer.setMinutesBalance(customer.getMinutesBalance() + amount.intValue());
                break;
            default:
                break;
        }
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }
}
