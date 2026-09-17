package com.zigi.ussd.service;

import com.zigi.ussd.model.Customer;
import com.zigi.ussd.model.ClientSession;
import com.zigi.ussd.model.OtpCode;
import com.zigi.ussd.repository.ClientSessionRepository;
import com.zigi.ussd.repository.CustomerRepository;
import com.zigi.ussd.repository.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    private static final Pattern MTN_RWANDA_PATTERN = Pattern.compile("^(?:\\+250|0)(7[89]\\d{7})$");

    private final OtpCodeRepository otpRepository;
    private final ClientSessionRepository sessionRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(OtpCodeRepository otpRepository,
                       ClientSessionRepository sessionRepository,
                       CustomerRepository customerRepository,
                       EmailService emailService) {
        this.otpRepository = otpRepository;
        this.sessionRepository = sessionRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
    }

    public static class RequestOtpResult {
        public boolean ok;
        public String error;
        public LocalDateTime expiresAt;
    }

    public static class VerifyResult {
        public boolean ok;
        public String error;
        public String token;
    }

    public RequestOtpResult requestOtp(String phoneNumber, String email, String deliveryMethod, String channel) {
        return requestOtp(phoneNumber, email, deliveryMethod, null, false, channel);
    }

    public RequestOtpResult requestOtp(String phoneNumber, String email, String deliveryMethod, String fullName, Boolean isRegister, String channel) {
        RequestOtpResult result = new RequestOtpResult();

        if (phoneNumber == null || phoneNumber.isBlank()) {
            result.error = "Phone number is required.";
            return result;
        }
        String normalizedPhone = normalizeMtnPhone(phoneNumber);
        if (normalizedPhone == null) {
            result.error = "Please enter a valid MTN Rwanda number (e.g. 078XXXXXXX, 079XXXXXXX, or +2507XXXXXXXX).";
            return result;
        }
        phoneNumber = normalizedPhone; 
        if (email == null || email.isBlank()) {
            result.error = "Email is required.";
            return result;
        }

        boolean registering = Boolean.TRUE.equals(isRegister) || (fullName != null && !fullName.isBlank());
        Optional<Customer> existingByPhone = customerRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber.trim());
        Optional<Customer> existingByEmail = customerRepository.findByEmailIgnoreCaseAndIsDeletedFalse(email.trim());

        String method = deliveryMethod;

        if (registering) {
            if (existingByPhone.isPresent()) {
                result.error = "An account with phone number " + phoneNumber + " is already registered. Please Sign In.";
                return result;
            }
            if (existingByEmail.isPresent()) {
                result.error = "An account with email address " + email + " is already registered. Please Sign In.";
                return result;
            }
            Customer newCustomer = new Customer();
            newCustomer.setFullName(fullName != null && !fullName.isBlank() ? fullName : "MTN Customer");
            newCustomer.setPhoneNumber(phoneNumber.trim());
            newCustomer.setEmail(email.trim());
            newCustomer.setMainBalance(new BigDecimal("1000.00"));
            newCustomer.setAirtimeBalance(new BigDecimal("500.00"));
            newCustomer.setDataBalanceMb(1024);
            newCustomer.setMinutesBalance(50);
            newCustomer.setIsDeleted(false);
            newCustomer.setCreatedAt(LocalDateTime.now());
            newCustomer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(newCustomer);
            log.info("[REGISTER] Created new customer for phone={} email={}", phoneNumber, email);

            method = "EMAIL";
        } else {
            
            if (existingByPhone.isEmpty() && existingByEmail.isEmpty()) {
                result.error = "No registered account found for that phone number or email address. Please click Register to create an account first!";
                return result;
            }
            method = ("EMAIL".equalsIgnoreCase(deliveryMethod)) ? "EMAIL" : "PHONE";
        }

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRY_MINUTES);

        OtpCode otp = new OtpCode();
        otp.setPhoneNumber(phoneNumber);
        otp.setEmail(email);
        otp.setDeliveryMethod(method);
        otp.setCode(code);
        otp.setChannel(channel != null ? channel : "UNKNOWN");
        otp.setExpiresAt(expiresAt);
        otpRepository.save(otp);

        sendCode(phoneNumber, email, code, channel, method);

        result.ok = true;
        result.expiresAt = expiresAt;
        return result;
    }

    private void sendCode(String phoneNumber, String email, String code, String channel, String method) {
        if ("EMAIL".equals(method)) {
            emailService.sendOtpEmail(email, code);
        } else {
            log.info("[OTP] channel={} phone={} code={}", channel, phoneNumber, code);
        }
    }

    public VerifyResult verifyOtp(String phoneNumber, String submittedCode, String channel) {
        VerifyResult result = new VerifyResult();

        String normalizedPhone = normalizeMtnPhone(phoneNumber);
        if (normalizedPhone == null) {
            result.error = "Please enter a valid MTN Rwanda number.";
            return result;
        }
        phoneNumber = normalizedPhone;

        List<OtpCode> history = otpRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
        Optional<OtpCode> latest = history.stream().filter(o -> !o.getConsumed()).findFirst();

        if (latest.isEmpty()) {
            result.error = "No code was requested for this number. Please request a new one.";
            return result;
        }

        OtpCode otp = latest.get();

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            result.error = "This code has expired. Please request a new one.";
            return result;
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            result.error = "Too many incorrect attempts. Please request a new code.";
            return result;
        }
        if (!otp.getCode().equals(submittedCode)) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            result.error = "Incorrect code. Please try again.";
            return result;
        }

        otp.setConsumed(true);
        otpRepository.save(otp);

        Optional<Customer> existingCustomer = customerRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber);
        if (existingCustomer.isEmpty()) {
            Customer newCustomer = new Customer();
            newCustomer.setFullName("MTN Customer (" + phoneNumber + ")");
            newCustomer.setPhoneNumber(phoneNumber);
            newCustomer.setMainBalance(new BigDecimal("1000.00"));
            newCustomer.setAirtimeBalance(new BigDecimal("500.00"));
            newCustomer.setDataBalanceMb(1024);
            newCustomer.setMinutesBalance(50);
            newCustomer.setIsDeleted(false);
            newCustomer.setCreatedAt(LocalDateTime.now());
            newCustomer.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(newCustomer);
        }

        ClientSession session = new ClientSession();
        session.setToken(UUID.randomUUID().toString());
        session.setPhoneNumber(phoneNumber);
        session.setChannel(channel != null ? channel : "UNKNOWN");
        sessionRepository.save(session);

        result.ok = true;
        result.token = session.getToken();
        return result;
    }

    public Optional<ClientSession> touchSession(String token) {
        Optional<ClientSession> sessionOpt = sessionRepository.findByTokenAndEndedAtIsNull(token);
        sessionOpt.ifPresent(s -> {
            s.setLastSeenAt(LocalDateTime.now());
            sessionRepository.save(s);
        });
        return sessionOpt;
    }

    public boolean endSession(String token) {
        Optional<ClientSession> sessionOpt = sessionRepository.findByTokenAndEndedAtIsNull(token);
        if (sessionOpt.isEmpty()) return false;
        ClientSession session = sessionOpt.get();
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
        return true;
    }

    private String generateCode() {
        int number = random.nextInt(1_000_000); 
        return String.format("%06d", number);
    }

    private String normalizeMtnPhone(String phoneNumber) {
        if (phoneNumber == null) return null;
        String cleaned = phoneNumber.trim().replaceAll("[\\s-]", "");
        Matcher matcher = MTN_RWANDA_PATTERN.matcher(cleaned);
        if (!matcher.matches()) return null;
        return "0" + matcher.group(1);
    }
}
