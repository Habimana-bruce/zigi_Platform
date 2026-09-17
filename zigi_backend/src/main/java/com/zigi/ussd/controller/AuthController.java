package com.zigi.ussd.controller;

import com.zigi.ussd.model.ClientSession;
import com.zigi.ussd.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public static class RequestOtpBody {
        public String phoneNumber;
        public String email;
        public String deliveryMethod; 
        public String fullName;       
        public Boolean isRegister;    
    }

    public static class VerifyOtpBody {
        public String phoneNumber;
        public String code;
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody RequestOtpBody body,
                                         @RequestHeader(value = "X-Client-Channel", required = false) String channel) {
        AuthService.RequestOtpResult result = authService.requestOtp(
                body.phoneNumber, body.email, body.deliveryMethod, body.fullName, body.isRegister, channel
        );
        if (!result.ok) {
            return ResponseEntity.badRequest().body(Map.of("message", result.error));
        }
        String destination = "EMAIL".equalsIgnoreCase(body.deliveryMethod) ? body.email : body.phoneNumber;
        return ResponseEntity.ok(Map.of(
                "message", "A verification code has been sent to " + destination + ".",
                "expiresAt", result.expiresAt.toString()
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpBody body,
                                        @RequestHeader(value = "X-Client-Channel", required = false) String channel) {
        if (body.phoneNumber == null || body.code == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "phoneNumber and code are required."));
        }
        AuthService.VerifyResult result = authService.verifyOtp(body.phoneNumber, body.code, channel);
        if (!result.ok) {
            return ResponseEntity.badRequest().body(Map.of("message", result.error));
        }
        return ResponseEntity.ok(Map.of("token", result.token, "phoneNumber", body.phoneNumber));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing Authorization header."));
        }
        boolean ended = authService.endSession(token);
        return ResponseEntity.ok(Map.of("ended", ended));
    }

    @GetMapping("/session")
    public ResponseEntity<?> currentSession(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Missing Authorization header."));
        }
        Optional<ClientSession> session = authService.touchSession(token);
        if (session.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Session expired or invalid."));
        }
        ClientSession s = session.get();
        return ResponseEntity.ok(Map.of("phoneNumber", s.getPhoneNumber(), "channel", s.getChannel()));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring("Bearer ".length()).trim();
    }
}
