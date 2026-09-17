package com.zigi.mobile.api;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 10000;

    public interface RawCallback {
        void onResult(boolean ok, int statusCode, String body, String headerXUssdStatus);
    }

    public static void get(String url, RawCallback callback) {
        request("GET", url, null, null, callback);
    }

    public static void postJson(String url, String jsonBody, RawCallback callback) {
        request("POST", url, jsonBody, "application/json", callback);
    }

    public static void postForm(String url, String formBody, RawCallback callback) {
        request("POST", url, formBody, "application/x-www-form-urlencoded", callback);
    }

    public static void putJson(String url, String jsonBody, RawCallback callback) {
        request("PUT", url, jsonBody, "application/json", callback);
    }

    public static void delete(String url, RawCallback callback) {
        request("DELETE", url, null, null, callback);
    }

    private static void request(String method, String urlStr, String body, String contentType, RawCallback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("X-Client-Channel", com.zigi.mobile.Config.CLIENT_CHANNEL);
                String token = com.zigi.mobile.Config.getAuthToken();
                if (token != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                if (body != null) {
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", contentType);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                }

                int status = conn.getResponseCode();
                boolean ok = status >= 200 && status < 300;
                String xUssdStatus = conn.getHeaderField("X-Ussd-Status");

                InputStream stream = ok ? conn.getInputStream() : conn.getErrorStream();
                String responseBody = readStream(stream);

                deliver(callback, ok, status, responseBody, xUssdStatus);
            } catch (IOException e) {
                deliver(callback, false, -1, "Could not reach server: " + e.getMessage(), null);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : sb.toString();
    }

    private static void deliver(RawCallback callback, boolean ok, int status, String body, String xUssdStatus) {
        MAIN_THREAD.post(() -> callback.onResult(ok, status, body, xUssdStatus));
    }
}
