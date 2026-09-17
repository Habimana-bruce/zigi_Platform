package com.zigi.ussd.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UssdSessionRequest {

    @NotBlank(message = "requestId is required")
    @Pattern(regexp = "^(0|1)$", message = "requestId must be exactly \"0\" or \"1\"")
    private String requestId;

    private String sessionId;

    @NotBlank(message = "phoneNumber (MSISDN) is required")
    private String phoneNumber;

    private String text = "";

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getText() { return text == null ? "" : text; }
    public void setText(String text) { this.text = text; }
}
