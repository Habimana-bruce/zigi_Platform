package com.zigi.ussd.controller;

import com.zigi.ussd.model.UssdSession;
import com.zigi.ussd.repository.UssdSessionRepository;
import com.zigi.ussd.service.UssdEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
public class UssdController {

    private final UssdEngine ussdEngine;
    private final UssdSessionRepository sessionRepository;

    @Value("${zigi.ussd.show-protocol-prefix:false}")
    private boolean showProtocolPrefix;

    public UssdController(UssdEngine ussdEngine, UssdSessionRepository sessionRepository) {
        this.ussdEngine = ussdEngine;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping(value = "/api/ussd", produces = MediaType.TEXT_PLAIN_VALUE)
    public org.springframework.http.ResponseEntity<String> handleUssd(
            @RequestParam String sessionId,
            @RequestParam String phoneNumber,
            @RequestParam(defaultValue = "") String text) {

        Optional<UssdSession> existing = sessionRepository.findById(sessionId);
        String priorText = existing.map(UssdSession::getAccumulatedText).orElse("");

        String accumulatedText;
        if (text == null || text.isEmpty()) {

            accumulatedText = "";
        } else if (existing.isEmpty()) {
            
            accumulatedText = text;
        } else {
            accumulatedText = priorText.isEmpty() ? text : priorText + "*" + text;
        }

        UssdEngine.UssdStepResult stepResult = ussdEngine.processStep(phoneNumber, accumulatedText);
        boolean isFinal = stepResult.response.startsWith("END ");

        if (isFinal) {
            sessionRepository.deleteById(sessionId);
        } else {
            UssdSession session = existing.orElseGet(UssdSession::new);
            session.setSessionId(sessionId);
            session.setPhoneNumber(phoneNumber);

            session.setAccumulatedText(stepResult.stepAccepted ? accumulatedText : priorText);
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        String body = showProtocolPrefix ? stepResult.response : stepResult.response.replaceFirst("^(CON|END)\\s", "");

        return org.springframework.http.ResponseEntity.ok()
                .header("X-Ussd-Status", isFinal ? "END" : "CON")
                .body(body);
    }
}
