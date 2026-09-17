package com.zigi.mobile.api;

import com.zigi.mobile.Config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UssdApi {

    private static final String USSD_URL = Config.API_BASE + "/api/ussd";

    public static class UssdResult {
        public boolean ok;
        public boolean isFinal;
        public String display;
    }

    public static void callUssd(String sessionId, String phoneNumber, String text, ApiCallback<UssdResult> callback) {
        String formBody = encode("sessionId", sessionId) + "&" + encode("phoneNumber", phoneNumber) + "&" + encode("text", text);

        HttpUtil.postForm(USSD_URL, formBody, (ok, status, body, xUssdStatus) -> {
            UssdResult result = new UssdResult();
            result.ok = ok;
            result.display = ok ? body : ("Could not reach server. Is the backend running at " + Config.API_BASE + "?");
            result.isFinal = ok ? "END".equals(xUssdStatus) : true;
            callback.onResult(ok, result);
        });
    }

    private static String encode(String key, String value) {
        try {
            return key + "=" + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return key + "=";
        }
    }
}
