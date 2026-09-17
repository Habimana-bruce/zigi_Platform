package com.zigi.mobile.api;

import android.os.Handler;
import android.os.Looper;

import com.zigi.mobile.Config;

import org.json.JSONObject;

public class PaymentApi {

    private static final String PAYMENTS_URL = Config.API_BASE + "/api/payments";
    private static final int MAX_ATTEMPTS = 8;
    private static final long INTERVAL_MS = 1500;

    public static void initiatePayment(String phoneNumber, int menuItemId, String provider, ApiCallback<JSONObject> callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("phoneNumber", phoneNumber);
            payload.put("menuItemId", menuItemId);
            payload.put("provider", provider);

            HttpUtil.postJson(PAYMENTS_URL, payload.toString(), (ok, status, body, ussdStatus) -> {
                try {
                    callback.onResult(ok, new JSONObject(ok ? body : "{}"));
                } catch (Exception e) {
                    callback.onResult(false, new JSONObject());
                }
            });
        } catch (Exception e) {
            callback.onResult(false, new JSONObject());
        }
    }

    public static void getPaymentStatus(long id, ApiCallback<JSONObject> callback) {
        HttpUtil.get(PAYMENTS_URL + "/" + id, (ok, status, body, ussdStatus) -> {
            try {
                callback.onResult(ok, ok ? new JSONObject(body) : null);
            } catch (Exception e) {
                callback.onResult(false, null);
            }
        });
    }

    public static void pollPaymentUntilDone(long id, ApiCallback<JSONObject> callback) {
        pollAttempt(id, 0, callback);
    }

    private static void pollAttempt(long id, int attempt, ApiCallback<JSONObject> callback) {
        if (attempt >= MAX_ATTEMPTS) {
            callback.onResult(false, null);
            return;
        }
        getPaymentStatus(id, (ok, data) -> {
            String status = (ok && data != null) ? data.optString("status", "") : "";
            if (ok && ("SUCCESS".equals(status) || "FAILED".equals(status))) {
                callback.onResult(true, data);
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> pollAttempt(id, attempt + 1, callback), INTERVAL_MS);
            }
        });
    }
}
