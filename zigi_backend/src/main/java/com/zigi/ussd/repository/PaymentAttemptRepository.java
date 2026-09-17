package com.zigi.ussd.repository;

import com.zigi.ussd.model.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    List<PaymentAttempt> findByTransactionIdOrderByAttemptNumberDesc(Long transactionId);
}
