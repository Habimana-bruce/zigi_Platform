package com.zigi.ussd.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "main_balance")
    private BigDecimal mainBalance = BigDecimal.ZERO;

    @Column(name = "airtime_balance")
    private BigDecimal airtimeBalance = BigDecimal.ZERO;

    @Column(name = "data_balance_mb")
    private Integer dataBalanceMb = 0;

    @Column(name = "minutes_balance")
    private Integer minutesBalance = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Customer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public BigDecimal getMainBalance() { return mainBalance; }
    public void setMainBalance(BigDecimal mainBalance) { this.mainBalance = mainBalance; }

    public BigDecimal getAirtimeBalance() { return airtimeBalance; }
    public void setAirtimeBalance(BigDecimal airtimeBalance) { this.airtimeBalance = airtimeBalance; }

    public Integer getDataBalanceMb() { return dataBalanceMb; }
    public void setDataBalanceMb(Integer dataBalanceMb) { this.dataBalanceMb = dataBalanceMb; }

    public Integer getMinutesBalance() { return minutesBalance; }
    public void setMinutesBalance(Integer minutesBalance) { this.minutesBalance = minutesBalance; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
