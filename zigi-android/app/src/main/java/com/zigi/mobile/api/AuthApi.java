package com.zigi.mobile.api;

import com.zigi.mobile.Config;

import org.json.JSONObject;

public class AuthApi {

    private static final String AUTH_URL = Config.API_BASE + "/api/auth";

    public static class OtpRequestResult {
        public boolean ok;
        public String devCode;   
        public String message;
    }

    public static class VerifyResult {
        public boolean ok;
        public String message;
    }

    public static void requestOtp(String phoneNumber, String email, String method, boolean isRegister, String fullName, ApiCallback<OtpRequestResult> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("phoneNumber", phoneNumber);
            payload.put("email", email);
            payload.put("deliveryMethod", method);
            payload.put("isRegister", isRegister);
            if (fullName != null) payload.put("fullName", fullName);

            HttpUtil.postJson(AUTH_URL + "/request-otp", payload.toString(), (ok, status, body, ussdStatus) -> {
                OtpRequestResult result = new OtpRequestResult();
                try {
                    JSONObject json = new JSONObject(body);
                    result.ok = ok;
                    result.devCode = json.optString("devCode", null);
                    result.message = json.optString("message", "");
                } catch (Exception e) {
                    result.ok = false;
                    result.message = "Could not reach server. Is the backend running at " + Config.API_BASE + "?";
                }
                callback.onResult(result.ok, result);
            });
        } catch (Exception e) {
            OtpRequestResult result = new OtpRequestResult();
            result.ok = false;
            result.message = "Could not build request.";
            callback.onResult(false, result);
        }
    }

    public static void verifyOtp(String phoneNumber, String code, ApiCallback<VerifyResult> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("phoneNumber", phoneNumber);
            payload.put("code", code);

            HttpUtil.postJson(AUTH_URL + "/verify-otp", payload.toString(), (ok, status, body, ussdStatus) -> {
                VerifyResult result = new VerifyResult();
                try {
                    JSONObject json = new JSONObject(body);
                    result.ok = ok;
                    if (ok) {
                        Config.setAuthToken(json.optString("token"));
                    } else {
                        result.message = json.optString("message", "Incorrect code.");
                    }
                } catch (Exception e) {
                    result.ok = false;
                    result.message = "Could not reach server. Is the backend running at " + Config.API_BASE + "?";
                }
                callback.onResult(result.ok, result);
            });
        } catch (Exception e) {
            VerifyResult result = new VerifyResult();
            result.ok = false;
            result.message = "Could not build request.";
            callback.onResult(false, result);
        }
    }

    public static void logout(Runnable onDone) {
        String token = Config.getAuthToken();
        if (token == null) {
            Config.clearAuthToken();
            onDone.run();
            return;
        }
        HttpUtil.postJson(AUTH_URL + "/logout", "{}", (ok, status, body, ussdStatus) -> {
            Config.clearAuthToken();
            onDone.run();
        });
    }
}
