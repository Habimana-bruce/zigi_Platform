package com.zigi.ussd.repository;

import com.zigi.ussd.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    Optional<Customer> findByEmailIgnoreCaseAndIsDeletedFalse(String email);

    List<Customer> findByIsDeletedFalse();

    List<Customer> findByIsDeletedTrue();
}
