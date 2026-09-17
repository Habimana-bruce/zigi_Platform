package com.zigi.ussd.controller;

import com.zigi.ussd.model.Customer;
import com.zigi.ussd.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Customer> getAll() {
        return repository.findByIsDeletedFalse();
    }

    @GetMapping("/deleted")
    public List<Customer> getDeleted() {
        return repository.findByIsDeletedTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(@PathVariable Long id) {
        Optional<Customer> customer = repository.findById(id);
        if (customer.isEmpty() || Boolean.TRUE.equals(customer.get().getIsDeleted())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer.get());
    }

    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<?> getByPhone(@PathVariable String phoneNumber) {
        Optional<Customer> customer = repository.findByPhoneNumberAndIsDeletedFalse(phoneNumber);
        if (customer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No account found for phone number " + phoneNumber));
        }
        return ResponseEntity.ok(customer.get());
    }

    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody Customer customer) {
        customer.setId(null); 
        customer.setIsDeleted(false);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        Customer saved = repository.save(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer updated) {
        return repository.findById(id).map(existing -> {
            existing.setFullName(updated.getFullName());
            existing.setPhoneNumber(updated.getPhoneNumber());
            existing.setEmail(updated.getEmail());
            existing.setMainBalance(updated.getMainBalance());
            existing.setAirtimeBalance(updated.getAirtimeBalance());
            existing.setDataBalanceMb(updated.getDataBalanceMb());
            if (updated.getIsDeleted() != null) existing.setIsDeleted(updated.getIsDeleted());
            existing.setUpdatedAt(LocalDateTime.now());
            Customer saved = repository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/soft-delete/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        return repository.findById(id).map(existing -> {
            existing.setIsDeleted(true);
            existing.setUpdatedAt(LocalDateTime.now());
            repository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Customer " + id + " soft-deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        return repository.findById(id).map(existing -> {
            existing.setIsDeleted(false);
            existing.setUpdatedAt(LocalDateTime.now());
            repository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Customer " + id + " restored"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> hardDelete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Customer " + id + " permanently deleted"));
    }
}
