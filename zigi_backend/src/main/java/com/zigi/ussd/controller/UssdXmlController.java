package com.zigi.ussd.controller;

import com.zigi.ussd.model.UssdXmlSession;
import com.zigi.ussd.repository.UssdXmlSessionRepository;
import com.zigi.ussd.service.UssdEngine;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
public class UssdXmlController {

    private final UssdEngine ussdEngine;
    private final UssdXmlSessionRepository sessionRepository;

    public UssdXmlController(UssdEngine ussdEngine, UssdXmlSessionRepository sessionRepository) {
        this.ussdEngine = ussdEngine;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping(
            value = "/api/ussd-xml",
            consumes = { MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, "text/plain" },
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public String handle(@RequestBody String rawXml) throws Exception {
        String msisdn = extractTag(rawXml, "msisdn");
        String sessionId = extractTag(rawXml, "sessionid");
        String input = extractTag(rawXml, "input");

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "unknown-session";
        }

        Optional<UssdXmlSession> existing = sessionRepository.findById(sessionId);
        String priorText = existing.map(UssdXmlSession::getAccumulatedText).orElse("");
        String accumulatedText;
        if (existing.isEmpty()) {
            
            accumulatedText = "";
        } else {
            accumulatedText = priorText.isEmpty() ? input : priorText + "*" + input;
        }

        UssdEngine.UssdStepResult stepResult = ussdEngine.processStep(msisdn, accumulatedText);
        boolean isFinal = stepResult.response.startsWith("END ");
        String display = stepResult.response.replaceFirst("^(CON|END)\\s", "");

        if (isFinal) {
            sessionRepository.deleteById(sessionId);
        } else {
            UssdXmlSession session = existing.orElseGet(UssdXmlSession::new);
            session.setSessionId(sessionId);
            session.setPhoneNumber(msisdn);

            session.setAccumulatedText(stepResult.stepAccepted ? accumulatedText : priorText);
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response type=\"pull\">\n" +
                "  <sessionid>" + escape(sessionId) + "</sessionid>\n" +
                "  <freeflow>\n" +
                "    <mode>FD</mode>\n" +
                "    <content>" + escape(display) + "</content>\n" +
                "    <action>" + (isFinal ? "end" : "request-input") + "</action>\n" +
                "  </freeflow>\n" +
                "</response>";
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

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
