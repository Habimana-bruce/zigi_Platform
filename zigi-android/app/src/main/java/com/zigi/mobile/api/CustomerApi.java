package com.zigi.mobile.api;

import com.zigi.mobile.Config;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CustomerApi {

    private static final String CUSTOMERS_URL = Config.API_BASE + "/api/customers";

    public static void getBalanceByPhone(String phone, ApiCallback<JSONObject> callback) {
        try {
            String url = CUSTOMERS_URL + "/phone/" + URLEncoder.encode(phone, StandardCharsets.UTF_8.name());
            HttpUtil.get(url, (ok, status, body, ussdStatus) -> {
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
}
