package com.zigi.mobile;

public class Config {
    public static final String API_BASE = "http://10.0.2.2:6157";
    public static final String CLIENT_CHANNEL = "MOBILE_ANDROID";

    private static String authToken = null;

    public static String getAuthToken() { return authToken; }
    public static void setAuthToken(String token) { authToken = token; }
    public static void clearAuthToken() { authToken = null; }
}
