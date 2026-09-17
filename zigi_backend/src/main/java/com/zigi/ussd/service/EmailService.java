package com.zigi.ussd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String code) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.info("[OTP-EMAIL] (no mail account configured, not actually sent) to={} code={}", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Your Zigi verification code");
            message.setText("Your Zigi verification code is: " + code
                    + "\n\nThis code expires in 5 minutes. If you didn't request this, you can ignore this email.");
            mailSender.send(message);
            log.info("[OTP-EMAIL] sent to={}", toEmail);
        } catch (Exception e) {

            log.warn("[OTP-EMAIL] failed to send to={} code={} error={}", toEmail, code, e.getMessage());
        }
    }
}
