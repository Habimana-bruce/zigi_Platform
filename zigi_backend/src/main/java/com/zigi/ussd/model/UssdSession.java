package com.zigi.ussd.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ussd_sessions")
public class UssdSession {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "accumulated_text", columnDefinition = "TEXT")
    private String accumulatedText = "";

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UssdSession() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAccumulatedText() { return accumulatedText; }
    public void setAccumulatedText(String accumulatedText) { this.accumulatedText = accumulatedText; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
