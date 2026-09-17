package com.zigi.ussd.controller;

import com.zigi.ussd.exception.InvalidSessionException;
import com.zigi.ussd.model.UssdSession;
import com.zigi.ussd.repository.UssdSessionRepository;
import com.zigi.ussd.service.UssdEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@Validated
public class UssdSessionController {

    private final UssdEngine ussdEngine;
    private final UssdSessionRepository sessionRepository;
    private final Validator validator;

    @Value("${zigi.ussd.session-ttl-seconds:120}")
    private long sessionTtlSeconds;

    public UssdSessionController(UssdEngine ussdEngine, UssdSessionRepository sessionRepository, Validator validator) {
        this.ussdEngine = ussdEngine;
        this.sessionRepository = sessionRepository;
        this.validator = validator;
    }

    @PostMapping(value = "/api/ussd/session", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UssdSessionResponse> handleJson(@Valid @RequestBody UssdSessionRequest request) {
        UssdSessionResponse result = route(request);
        return withSessionHeader(result);
    }

    @PostMapping(value = "/api/ussd/session", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<UssdSessionResponse> handleForm(
            @RequestParam String requestId,
            @RequestParam(required = false) String sessionId,
            @RequestParam String phoneNumber,
            @RequestParam(defaultValue = "") String text) {

        UssdSessionRequest request = new UssdSessionRequest();
        request.setRequestId(requestId);
        request.setSessionId(sessionId);
        request.setPhoneNumber(phoneNumber);
        request.setText(text);

        Set<ConstraintViolation<UssdSessionRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream().findFirst()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .orElse("Invalid request");
            throw new InvalidSessionException(message);
        }

        UssdSessionResponse result = route(request);
        return withSessionHeader(result);
    }

    private ResponseEntity<UssdSessionResponse> withSessionHeader(UssdSessionResponse result) {
        String sessionId = result.getSessionId();
        result.setSessionId(null); 
        return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)
                .body(result);
    }

    @PostMapping(
            value = "/api/ussd/session",
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE },
            produces = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE }
    )
    public ResponseEntity<String> handleXml(@RequestBody String rawXml) throws Exception {
        UssdSessionRequest request = new UssdSessionRequest();
        request.setRequestId(extractTag(rawXml, "requestId"));
        request.setSessionId(extractTag(rawXml, "sessionId"));
        request.setPhoneNumber(extractTag(rawXml, "phoneNumber"));
        request.setText(extractTag(rawXml, "text"));

        Set<ConstraintViolation<UssdSessionRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream().findFirst()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .orElse("Invalid request");
            throw new InvalidSessionException(message);
        }

        UssdSessionResponse result = route(request);

        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "  <message>" + escapeXml(result.getMessage()) + "</message>\n" +
                "</response>";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header("X-Session-Id", result.getSessionId())
                .body(body);
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String extractTag(String xml, String tagName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); 
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return "";
        Element el = (Element) nodes.item(0);
        String text = el.getTextContent();
        return text == null ? "" : text.trim();
    }

    private UssdSessionResponse route(UssdSessionRequest request) {
        if ("1".equals(request.getRequestId())) {
            return startNewSession(request);
        }
        return continueSession(request);
    }

    private UssdSessionResponse startNewSession(UssdSessionRequest request) {
        String sessionId;
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {

            if (sessionRepository.existsById(request.getSessionId())) {
                throw new InvalidSessionException(
                        "sessionId \"" + request.getSessionId() + "\" is already in use by an active session. " +
                        "Choose a different value, or leave sessionId out to have one generated automatically.");
            }
            sessionId = request.getSessionId();
        } else {
            sessionId = UUID.randomUUID().toString();
        }

        String result = ussdEngine.processStep(request.getPhoneNumber(), "").response; 
        boolean isFinal = result.startsWith("END "); 
        String message = result.replaceFirst("^(CON|END)\\s", "");

        if (!isFinal) {
            UssdSession session = new UssdSession();
            session.setSessionId(sessionId);
            session.setPhoneNumber(request.getPhoneNumber());
            session.setAccumulatedText("");
            session.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTtlSeconds));
            sessionRepository.save(session);
        }

        return new UssdSessionResponse(sessionId, message);
    }

    private UssdSessionResponse continueSession(UssdSessionRequest request) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new InvalidSessionException("sessionId is required when requestId is \"0\"");
        }

        Optional<UssdSession> existing = sessionRepository.findById(request.getSessionId());
        if (existing.isEmpty()) {
            throw new InvalidSessionException("Session not found. It may have already ended - start a new session with requestId=\"1\".");
        }
        UssdSession session = existing.get();
        if (session.isExpired()) {
            sessionRepository.deleteById(session.getSessionId());
            throw new InvalidSessionException("Session has expired.");
        }

        String prev = session.getAccumulatedText();
        String newText = request.getText();
        String accumulatedText = (prev == null || prev.isEmpty())
                ? newText
                : (newText == null || newText.isEmpty() ? prev : prev + "*" + newText);

        UssdEngine.UssdStepResult stepResult = ussdEngine.processStep(request.getPhoneNumber(), accumulatedText);
        boolean isFinal = stepResult.response.startsWith("END ");
        String message = stepResult.response.replaceFirst("^(CON|END)\\s", "");

        if (isFinal) {
            sessionRepository.deleteById(session.getSessionId());
        } else {

            session.setAccumulatedText(stepResult.stepAccepted ? accumulatedText : prev);
            session.setExpiresAt(LocalDateTime.now().plusSeconds(sessionTtlSeconds)); 
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        return new UssdSessionResponse(session.getSessionId(), message);
    }
}
