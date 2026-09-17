package com.zigi.mobile.api;

public interface ApiCallback<T> {
    void onResult(boolean ok, T data);
}
