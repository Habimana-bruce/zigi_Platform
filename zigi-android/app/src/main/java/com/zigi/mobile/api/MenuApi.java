package com.zigi.mobile.api;

import com.zigi.mobile.Config;
import com.zigi.mobile.model.MenuOption;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class MenuApi {

    private static final String MENU_URL = Config.API_BASE + "/api/menu";

    public static void getMenuRoots(String group, ApiCallback<List<MenuOption>> callback) {
        try {
            String url = MENU_URL + "/roots/" + URLEncoder.encode(group, StandardCharsets.UTF_8.name());
            HttpUtil.get(url, (ok, status, body, ussdStatus) -> {
                if (!ok) { callback.onResult(false, Collections.emptyList()); return; }
                try {
                    callback.onResult(true, MenuOption.listFromJson(body));
                } catch (Exception e) {
                    callback.onResult(false, Collections.emptyList());
                }
            });
        } catch (Exception e) {
            callback.onResult(false, Collections.emptyList());
        }
    }

    public static void getMenuChildren(int parentId, ApiCallback<List<MenuOption>> callback) {
        String url = MENU_URL + "/" + parentId + "/children";
        HttpUtil.get(url, (ok, status, body, ussdStatus) -> {
            if (!ok) { callback.onResult(false, Collections.emptyList()); return; }
            try {
                callback.onResult(true, MenuOption.listFromJson(body));
            } catch (Exception e) {
                callback.onResult(false, Collections.emptyList());
            }
        });
    }
}
