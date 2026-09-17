package com.zigi.mobile.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuData {

    public static final List<String> SUGGESTED_PROMPTS = Arrays.asList(
            "Check my data usage",
            "I want to buy data",
            "I want to check my balance"
    );

    public static final List<String> QUICK_BUTTONS_MAIN = Arrays.asList(
            "\uD83D\uDCA1 Airtime", "\uD83D\uDCB3 Data", "\uD83E\uDDD1\u200D\uD83D\uDCBC Assist", "Menu", "Help", "Exit"
    );

    public static final List<String> QUICK_BUTTONS_HELP = Arrays.asList("Menu", "Help", "Exit");

    public static final Map<String, String> GROUP_TITLES = new HashMap<>();
    static {
        GROUP_TITLES.put("MAIN", "You can also select other options with the clickable buttons");
        GROUP_TITLES.put("HELP", "Please select the support option you need");
        GROUP_TITLES.put("DATA", "Choose a data bundle");
        GROUP_TITLES.put("AIRTIME", "Airtime options");
    }

    public static final List<String> GREETINGS = Arrays.asList(
            "hi", "hello", "hy", "hey", "yello", "mwaramutse", "muraho"
    );
}
