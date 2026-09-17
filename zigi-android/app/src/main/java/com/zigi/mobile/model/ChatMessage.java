package com.zigi.mobile.model;

import java.util.List;
import java.util.UUID;

public class ChatMessage {

    public enum Type { USER, BOT, MENU, SUGGESTIONS, IDENTIFY_FORM, OTP_FORM, USSD }

    public final String id = UUID.randomUUID().toString();
    public Type type;

    public String text;                     
    public String menuTitle;                
    public List<MenuOption> menuOptions;    
    public List<String> suggestions;        
    public boolean identifyActive;          
    public String otpPhone;                 
    public String otpEmail;                 
    public String otpMethod;                
    public String otpDevCode;               
    public String otpError;                 
    public boolean otpActive;               

    public static ChatMessage user(String text) {
        ChatMessage m = new ChatMessage();
        m.type = Type.USER;
        m.text = text;
        return m;
    }

    public static ChatMessage bot(String text) {
        ChatMessage m = new ChatMessage();
        m.type = Type.BOT;
        m.text = text;
        return m;
    }

    public static ChatMessage ussd(String text) {
        ChatMessage m = new ChatMessage();
        m.type = Type.USSD;
        m.text = text;
        return m;
    }

    public static ChatMessage menu(String title, List<MenuOption> options) {
        ChatMessage m = new ChatMessage();
        m.type = Type.MENU;
        m.menuTitle = title;
        m.menuOptions = options;
        return m;
    }

    public static ChatMessage suggestions(List<String> suggestions) {
        ChatMessage m = new ChatMessage();
        m.type = Type.SUGGESTIONS;
        m.suggestions = suggestions;
        return m;
    }

    public static ChatMessage identifyForm(boolean active) {
        ChatMessage m = new ChatMessage();
        m.type = Type.IDENTIFY_FORM;
        m.identifyActive = active;
        return m;
    }

    public static ChatMessage otpForm(String phone, String email, String method, String devCode, boolean active) {
        ChatMessage m = new ChatMessage();
        m.type = Type.OTP_FORM;
        m.otpPhone = phone;
        m.otpEmail = email;
        m.otpMethod = method;
        m.otpDevCode = devCode;
        m.otpActive = active;
        return m;
    }

    public static class MenuHolder {
        public final String title;
        public final List<MenuOption> options;
        public MenuHolder(String title, List<MenuOption> options) {
            this.title = title;
            this.options = options;
        }
    }
}
