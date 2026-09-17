package com.zigi.mobile.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MenuOption {
    public int id;
    public String label = "";
    public String icon = "";
    public boolean hasChildren;
    public String actionType = "";      
    public String balanceType = "";     
    public String responseText = "";
    public double price;

    public static MenuOption fromJson(JSONObject o) {
        MenuOption m = new MenuOption();
        m.id = o.optInt("id");
        m.label = o.optString("label", "");
        m.icon = o.optString("icon", "");
        m.hasChildren = o.optBoolean("hasChildren", false);
        m.actionType = o.optString("actionType", "");
        m.balanceType = o.optString("balanceType", "");
        m.responseText = o.optString("responseText", "");
        m.price = o.optDouble("price", 0);
        return m;
    }

    public static List<MenuOption> listFromJson(String jsonArrayStr) throws JSONException {
        List<MenuOption> list = new ArrayList<>();
        JSONArray arr = new JSONArray(jsonArrayStr);
        for (int i = 0; i < arr.length(); i++) {
            list.add(fromJson(arr.getJSONObject(i)));
        }
        return list;
    }
}
