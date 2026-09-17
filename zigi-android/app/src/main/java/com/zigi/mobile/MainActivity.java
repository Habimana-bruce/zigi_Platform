package com.zigi.mobile;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zigi.mobile.adapter.ChatAdapter;
import com.zigi.mobile.api.AuthApi;
import com.zigi.mobile.api.CustomerApi;
import com.zigi.mobile.api.MenuApi;
import com.zigi.mobile.api.PaymentApi;
import com.zigi.mobile.api.UssdApi;
import com.zigi.mobile.data.MenuData;
import com.zigi.mobile.model.ChatMessage;
import com.zigi.mobile.model.MenuOption;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ChatAdapter.Listener {

    private static final String USSD_DEMO_PHONE = "0700000000";

    private View welcomeScreen, chatScreen;
    private RecyclerView chatRecyclerView;
    private LinearLayout quickButtonsContainer;
    private EditText messageInput;

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;

    private String customerPhone = null;
    private String activeGroup = "MAIN";

    private ChatMessage.MenuHolder currentMenu = null;
    private final Deque<ChatMessage.MenuHolder> menuStack = new ArrayDeque<>();

    private boolean ussdMode = false;
    private String ussdSessionId = UUID.randomUUID().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeScreen = findViewById(R.id.welcomeScreen);
        chatScreen = findViewById(R.id.chatScreen);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        quickButtonsContainer = findViewById(R.id.quickButtonsContainer);
        messageInput = findViewById(R.id.messageInput);

        findViewById(R.id.startButton).setOnClickListener(v -> openChat());
        findViewById(R.id.backButton).setOnClickListener(v -> showWelcome());
        findViewById(R.id.sendButton).setOnClickListener(v -> handleSend());

        adapter = new ChatAdapter(messages, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);
    }

    private void openChat() {
        welcomeScreen.setVisibility(View.GONE);
        chatScreen.setVisibility(View.VISIBLE);
        if (messages.isEmpty()) {
            resetSession();
        }
    }

    private void showWelcome() {
        chatScreen.setVisibility(View.GONE);
        welcomeScreen.setVisibility(View.VISIBLE);
    }

    private void addMessage(ChatMessage msg) {
        messages.add(msg);
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
    }

    private void refreshAll() {

        adapter.notifyDataSetChanged();
    }

    private void resetSession() {
        messages.clear();
        adapter.notifyDataSetChanged();
        customerPhone = null;
        activeGroup = "MAIN";
        currentMenu = null;
        menuStack.clear();
        ussdMode = false;
        ussdSessionId = UUID.randomUUID().toString();
        Config.clearAuthToken();

        addMessage(ChatMessage.identifyForm(true));
        renderQuickButtons(java.util.Arrays.asList("Menu", "Help", "Exit"));
    }

    private void sendOtpFor(String phone, String email, String method, boolean isRegister, String fullName) {
        for (ChatMessage m : messages) {
            if (m.type == ChatMessage.Type.IDENTIFY_FORM) m.identifyActive = false;
        }
        refreshAll();
        AuthApi.requestOtp(phone, email, method, isRegister, fullName, (ok, result) -> {
            if (!ok) {
                addMessage(ChatMessage.bot("\u26A0\uFE0F " + result.message));
                addMessage(ChatMessage.identifyForm(true));
                return;
            }
            addMessage(ChatMessage.otpForm(phone, email, method, null, true));
        });
    }

    @Override
    public void onIdentifySubmit(String phone, String email, String method, boolean isRegister, String fullName) {
        sendOtpFor(phone, email, method, isRegister, fullName);
    }

    @Override
    public void onOtpVerify(String phone, String code) {
        ChatMessage otpMsg = findLatestOtpMessage(phone);
        AuthApi.verifyOtp(phone, code, (ok, result) -> {
            if (!ok) {
                if (otpMsg != null) {
                    otpMsg.otpError = result.message;
                    refreshAll();
                }
                return;
            }
            if (otpMsg != null) {
                otpMsg.otpActive = false;
                otpMsg.otpError = null;
                refreshAll();
            }
            customerPhone = phone;
            addMessage(ChatMessage.bot("\u2705 Authenticated! Welcome, we'll use <b>" + phone + "</b> for your account actions in this chat."));
            addMessage(ChatMessage.suggestions(MenuData.SUGGESTED_PROMPTS));
        });
    }

    @Override
    public void onOtpResend(String phone) {
        ChatMessage otpMsg = findLatestOtpMessage(phone);
        String email = otpMsg != null ? otpMsg.otpEmail : null;
        String method = otpMsg != null ? otpMsg.otpMethod : "PHONE";
        AuthApi.requestOtp(phone, email, method, false, null, (ok, result) -> {
            if (otpMsg == null) return;
            if (ok) {
                otpMsg.otpError = null;
                refreshAll();
                addMessage(ChatMessage.bot("\uD83D\uDCE9 A new code has been sent."));
            } else {
                otpMsg.otpError = result.message;
                refreshAll();
            }
        });
    }

    private ChatMessage findLatestOtpMessage(String phone) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (m.type == ChatMessage.Type.OTP_FORM && phone.equals(m.otpPhone)) return m;
        }
        return null;
    }

    private boolean requireAuth() {
        if (customerPhone != null) return true;
        addMessage(ChatMessage.bot("\uD83D\uDD12 Please sign in or register first so we can verify your account."));
        boolean alreadyOpen = false;
        for (ChatMessage m : messages) {
            if ((m.type == ChatMessage.Type.IDENTIFY_FORM && m.identifyActive)
                    || (m.type == ChatMessage.Type.OTP_FORM && m.otpActive)) {
                alreadyOpen = true;
                break;
            }
        }
        if (!alreadyOpen) {
            addMessage(ChatMessage.identifyForm(true));
        }
        return false;
    }

    private void showMenuGroup(String group) {
        MenuApi.getMenuRoots(group, (ok, items) -> {
            if (!ok) {
                addMessage(ChatMessage.bot("\u26A0\uFE0F Could not load the menu. Check your backend URL in Config.java."));
                return;
            }
            if (items.isEmpty()) {
                addMessage(ChatMessage.bot("No items found for \"" + group + "\" yet. Add some via /api/menu."));
                return;
            }
            String title = MenuData.GROUP_TITLES.containsKey(group) ? MenuData.GROUP_TITLES.get(group) : group;
            addMessage(ChatMessage.menu(title, items));
            activeGroup = group;
            currentMenu = new ChatMessage.MenuHolder(title, items);
            menuStack.clear();
            renderQuickButtons(group.equals("HELP") ? MenuData.QUICK_BUTTONS_HELP : MenuData.QUICK_BUTTONS_MAIN);
        });
    }

    @Override
    public void onBack() {
        if (menuStack.isEmpty()) {
            addMessage(ChatMessage.bot("You're already at the top of this menu."));
            if (currentMenu != null) addMessage(ChatMessage.menu(currentMenu.title, currentMenu.options));
            return;
        }
        ChatMessage.MenuHolder previous = menuStack.pop();
        currentMenu = previous;
        addMessage(ChatMessage.menu(previous.title, previous.options));
    }

    @Override
    public void onMenuOptionClick(MenuOption option) {
        addMessage(ChatMessage.user(option.label));
        if (!requireAuth()) return;

        if ("BALANCE".equals(option.actionType) || "PAYMENT".equals(option.actionType)) {
            if ("BALANCE".equals(option.actionType)) {
                handleBalanceCheck(option);
            } else {
                handlePaymentSubmit(option);
            }
            return;
        }

        if (option.hasChildren) {
            MenuApi.getMenuChildren(option.id, (ok, items) -> {
                if (!ok) {
                    addMessage(ChatMessage.bot("\u26A0\uFE0F Could not load that submenu. Check your backend connection."));
                    return;
                }
                if (currentMenu != null) menuStack.push(currentMenu);
                currentMenu = new ChatMessage.MenuHolder(option.label, items);
                addMessage(ChatMessage.menu(option.label, items));
            });
            return;
        }

        addMessage(ChatMessage.bot(!option.responseText.isEmpty() ? option.responseText : "You selected <b>" + option.label + "</b>."));
        showMenuGroup(activeGroup);
    }

    private void handleBalanceCheck(MenuOption item) {
        addMessage(ChatMessage.bot("\u23F3 Your request is being processed..."));
        CustomerApi.getBalanceByPhone(customerPhone, (ok, data) -> {
            if (!ok) {
                addMessage(ChatMessage.bot("\u23F3 Your request is being processed. You will receive your balance information via SMS shortly."));
            } else if ("AIRTIME".equals(item.balanceType)) {
                addMessage(ChatMessage.bot("<b>Airtime Balance</b><br>" + data.optString("airtimeBalance") + " RWF"));
            } else if ("DATA".equals(item.balanceType)) {
                addMessage(ChatMessage.bot("<b>Data Balance</b><br>" + data.optString("dataBalanceMb") + " MB"));
            } else {
                addMessage(ChatMessage.bot("<b>Balance Summary</b><br>Name: " + data.optString("fullName")
                        + "<br>Main Balance: " + data.optString("mainBalance") + " RWF"
                        + "<br>Airtime: " + data.optString("airtimeBalance") + " RWF"
                        + "<br>Data: " + data.optString("dataBalanceMb") + " MB"));
            }
            showMenuGroup(activeGroup);
        });
    }

    private void handlePaymentSubmit(MenuOption item) {
        addMessage(ChatMessage.bot("\u23F3 Your request is being processed - confirming purchase of <b>" + item.label
                + "</b> (" + item.price + " RWF) via MTN MoMo..."));

        PaymentApi.initiatePayment(customerPhone, item.id, "MTN", (ok, data) -> {
            if (!ok) {
                addMessage(ChatMessage.bot("\u26A0\uFE0F " + (data != null ? data.optString("message", "Could not start the payment.") : "Could not start the payment.")));
                showMenuGroup(activeGroup);
                return;
            }
            long txnId = data.optLong("id");
            PaymentApi.pollPaymentUntilDone(txnId, (pollOk, result) -> {
                if (!pollOk) {
                    addMessage(ChatMessage.bot("Still waiting on confirmation for transaction TXN" + txnId + ". Check back shortly - it may complete after this chat."));
                } else if ("SUCCESS".equals(result.optString("status"))) {
                    addMessage(ChatMessage.bot("\u2705 Payment confirmed! You've purchased <b>" + item.label + "</b>. Check My Balance to see your updated balance."));
                } else {
                    addMessage(ChatMessage.bot("\u274C Payment failed for <b>" + item.label + "</b>. Please try again."));
                }
                showMenuGroup(activeGroup);
            });
        });
    }

    private void renderQuickButtons(List<String> buttons) {
        quickButtonsContainer.removeAllViews();
        for (String label : buttons) {
            TextView btn = new TextView(this);
            btn.setText(label);
            btn.setTextSize(13);
            btn.setPadding(28, 16, 28, 16);
            btn.setBackgroundResource(R.drawable.quick_button_bg);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(16);
            btn.setLayoutParams(params);
            btn.setOnClickListener(v -> handleQuickButtonClick(label));
            quickButtonsContainer.addView(btn);
        }
    }

    private void handleQuickButtonClick(String label) {
        addMessage(ChatMessage.user(label));

        if (label.contains("Exit")) {
            AuthApi.logout(() -> {
                resetSession();
                addMessage(ChatMessage.bot("Thanks for using Zigi! \uD83D\uDC4B Session ended - enter a phone number to start again."));
            });
            return;
        }

        if (!requireAuth()) return;

        if (label.contains("Menu")) {
            showMenuGroup("MAIN");
        } else if (label.contains("Help")) {
            showMenuGroup("HELP");
        } else if (label.contains("Data")) {
            showMenuGroup("DATA");
        } else if (label.contains("Airtime")) {
            showMenuGroup("AIRTIME");
        } else if (label.contains("Assist")) {
            MenuApi.getMenuRoots("ASSIST", (ok, items) -> {
                if (!ok) {
                    addMessage(ChatMessage.bot("\u26A0\uFE0F Could not reach the server. Check your backend connection."));
                    return;
                }
                if (items.size() == 1 && !items.get(0).hasChildren) {
                    addMessage(ChatMessage.bot(items.get(0).responseText));
                    showMenuGroup(activeGroup);
                } else if (!items.isEmpty()) {
                    currentMenu = new ChatMessage.MenuHolder("Assist", items);
                    menuStack.clear();
                    addMessage(ChatMessage.menu("Assist", items));
                } else {
                    showMenuGroup(activeGroup);
                }
            });
        } else {
            addMessage(ChatMessage.bot("You selected <b>" + label + "</b>. (Demo response \u2014 wire this up to a real backend action.)"));
            showMenuGroup(activeGroup);
        }
    }

    private void startUssd() {
        ussdMode = true;
        UssdApi.callUssd(ussdSessionId, USSD_DEMO_PHONE, "", (ok, result) -> {
            addMessage(ChatMessage.ussd(result.display));
            if (result.isFinal) ussdMode = false;
        });
    }

    private void handleUssdStep(String step) {
        UssdApi.callUssd(ussdSessionId, USSD_DEMO_PHONE, step, (ok, result) -> {
            addMessage(ChatMessage.ussd(result.display));
            if (result.isFinal) ussdMode = false;
        });
    }

    private void handleSend() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        messageInput.setText("");

        addMessage(ChatMessage.user(text));

        if (ussdMode) {
            handleUssdStep(text);
            return;
        }

        String normalized = text.toLowerCase().replaceAll("[^a-z]", "");
        if (MenuData.GREETINGS.contains(normalized)) {
            addMessage(ChatMessage.suggestions(MenuData.SUGGESTED_PROMPTS));
            return;
        }

        addMessage(ChatMessage.bot("Got it! Use the buttons below or type 'menu' to see options."));
        if (!requireAuth()) return;
        showMenuGroup(activeGroup);
    }
}
