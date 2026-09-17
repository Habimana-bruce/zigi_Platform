package com.zigi.mobile.adapter;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zigi.mobile.R;
import com.zigi.mobile.model.ChatMessage;
import com.zigi.mobile.model.MenuOption;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onMenuOptionClick(MenuOption option);
        void onBack();
        void onIdentifySubmit(String phone, String email, String method, boolean isRegister, String fullName);
        void onOtpVerify(String phone, String code);
        void onOtpResend(String phone);
    }

    private final List<ChatMessage> messages;
    private final Listener listener;

    public ChatAdapter(List<ChatMessage> messages, Listener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type.ordinal();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ChatMessage.Type type = ChatMessage.Type.values()[viewType];
        switch (type) {
            case USER:
                return new TextVH(inflater.inflate(R.layout.item_bubble_user, parent, false));
            case USSD:
                return new TextVH(inflater.inflate(R.layout.item_bubble_ussd, parent, false));
            case MENU:
                return new MenuVH(inflater.inflate(R.layout.item_bubble_menu, parent, false));
            case SUGGESTIONS:
                return new SuggestionsVH(inflater.inflate(R.layout.item_bubble_suggestions, parent, false));
            case IDENTIFY_FORM:
                return new IdentifyVH(inflater.inflate(R.layout.item_bubble_identify, parent, false));
            case OTP_FORM:
                return new OtpVH(inflater.inflate(R.layout.item_bubble_otp, parent, false));
            case BOT:
            default:
                return new TextVH(inflater.inflate(R.layout.item_bubble_bot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof TextVH) {
            ((TextVH) holder).bind(msg);
        } else if (holder instanceof MenuVH) {
            ((MenuVH) holder).bind(msg, listener);
        } else if (holder instanceof SuggestionsVH) {
            ((SuggestionsVH) holder).bind(msg);
        } else if (holder instanceof IdentifyVH) {
            ((IdentifyVH) holder).bind(msg, listener);
        } else if (holder instanceof OtpVH) {
            ((OtpVH) holder).bind(msg, listener);
        }
    }

    static class TextVH extends RecyclerView.ViewHolder {
        final TextView text;
        TextVH(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
        }
        void bind(ChatMessage msg) {
            if (msg.type == ChatMessage.Type.BOT) {
                text.setText(Html.fromHtml(msg.text == null ? "" : msg.text, Html.FROM_HTML_MODE_LEGACY));
            } else {
                text.setText(msg.text);
            }
        }
    }

    static class MenuVH extends RecyclerView.ViewHolder {
        final TextView title;
        final LinearLayout optionsContainer;
        final TextView backOption;

        MenuVH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
            backOption = itemView.findViewById(R.id.backOption);
        }

        void bind(ChatMessage msg, Listener listener) {
            title.setText(msg.menuTitle + " \uD83D\uDC47");
            optionsContainer.removeAllViews();

            List<MenuOption> options = msg.menuOptions;
            for (int i = 0; i < options.size(); i++) {
                MenuOption option = options.get(i);
                TextView row = new TextView(itemView.getContext());
                row.setText((i + 1) + ". " + option.icon + " " + option.label);
                row.setTextColor(itemView.getContext().getColor(R.color.text_dark));
                row.setTextSize(14);
                row.setPadding(0, 12, 0, 12);
                row.setOnClickListener(v -> listener.onMenuOptionClick(option));
                optionsContainer.addView(row);
            }

            backOption.setOnClickListener(v -> listener.onBack());
        }
    }

    static class SuggestionsVH extends RecyclerView.ViewHolder {
        final LinearLayout promptsContainer;
        SuggestionsVH(View itemView) {
            super(itemView);
            promptsContainer = itemView.findViewById(R.id.promptsContainer);
        }
        void bind(ChatMessage msg) {
            promptsContainer.removeAllViews();
            for (String prompt : msg.suggestions) {
                TextView row = new TextView(itemView.getContext());
                row.setText(prompt);
                row.setTextColor(itemView.getContext().getColor(R.color.text_dark));
                row.setTextSize(14);
                row.setTypeface(row.getTypeface(), android.graphics.Typeface.BOLD);
                row.setPadding(0, 10, 0, 0);
                promptsContainer.addView(row);
            }
        }
    }

    static class IdentifyVH extends RecyclerView.ViewHolder {
        final LinearLayout formContainer;
        final Button tabLoginButton;
        final Button tabRegisterButton;
        final EditText fullNameInput;
        final EditText phoneInput;
        final EditText emailInput;
        final LinearLayout deliveryMethodGroup;
        final Button methodPhoneButton;
        final Button methodEmailButton;
        final TextView registerEmailNotice;
        final Button submitButton;
        String selectedMethod = "PHONE";
        boolean isRegisterMode = false;

        IdentifyVH(View itemView) {
            super(itemView);
            formContainer = itemView.findViewById(R.id.formContainer);
            tabLoginButton = itemView.findViewById(R.id.tabLoginButton);
            tabRegisterButton = itemView.findViewById(R.id.tabRegisterButton);
            fullNameInput = itemView.findViewById(R.id.fullNameInput);
            phoneInput = itemView.findViewById(R.id.phoneInput);
            emailInput = itemView.findViewById(R.id.emailInput);
            deliveryMethodGroup = itemView.findViewById(R.id.deliveryMethodGroup);
            methodPhoneButton = itemView.findViewById(R.id.methodPhoneButton);
            methodEmailButton = itemView.findViewById(R.id.methodEmailButton);
            registerEmailNotice = itemView.findViewById(R.id.registerEmailNotice);
            submitButton = itemView.findViewById(R.id.submitButton);
        }

        void bind(ChatMessage msg, Listener listener) {
            formContainer.setVisibility(msg.identifyActive ? View.VISIBLE : View.GONE);
            selectedMethod = "PHONE";
            isRegisterMode = false;
            highlightTab();
            highlightMethod();

            tabLoginButton.setOnClickListener(v -> { isRegisterMode = false; highlightTab(); });
            tabRegisterButton.setOnClickListener(v -> { isRegisterMode = true; highlightTab(); });
            methodPhoneButton.setOnClickListener(v -> { selectedMethod = "PHONE"; highlightMethod(); });
            methodEmailButton.setOnClickListener(v -> { selectedMethod = "EMAIL"; highlightMethod(); });

            submitButton.setOnClickListener(v -> {
                String fullName = fullNameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();

                if (phone.isEmpty() || email.isEmpty() || (isRegisterMode && fullName.isEmpty())) {
                    String msg2 = isRegisterMode
                            ? "Please enter your full name, phone number, and email"
                            : "Please enter both phone number and email";
                    android.widget.Toast.makeText(itemView.getContext(), msg2, android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidMtnPhone(phone)) {
                    android.widget.Toast.makeText(itemView.getContext(),
                            "Please enter a valid MTN Rwanda number (078XXXXXXX, 079XXXXXXX, or +2507XXXXXXXX)",
                            android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                
                String method = isRegisterMode ? "EMAIL" : selectedMethod;
                listener.onIdentifySubmit(phone, email, method, isRegisterMode, isRegisterMode ? fullName : null);
            });
        }

        private static boolean isValidMtnPhone(String phone) {
            String cleaned = phone.replaceAll("[\\s-]", "");
            return cleaned.matches("^(?:\\+250|0)(7[89]\\d{7})$");
        }

        private void highlightTab() {
            int yellow = itemView.getContext().getResources().getColor(R.color.mtn_yellow);
            int gray = itemView.getContext().getResources().getColor(R.color.input_bg);
            tabLoginButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(!isRegisterMode ? yellow : gray));
            tabRegisterButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isRegisterMode ? yellow : gray));

            fullNameInput.setVisibility(isRegisterMode ? View.VISIBLE : View.GONE);
            deliveryMethodGroup.setVisibility(isRegisterMode ? View.GONE : View.VISIBLE);
            registerEmailNotice.setVisibility(isRegisterMode ? View.VISIBLE : View.GONE);
            submitButton.setText(isRegisterMode ? "CREATE ACCOUNT & VERIFY EMAIL \uD83D\uDE80" : "SIGN IN & GET OTP \uD83D\uDD12");
        }

        private void highlightMethod() {
            int yellow = itemView.getContext().getResources().getColor(R.color.mtn_yellow);
            int gray = itemView.getContext().getResources().getColor(R.color.input_bg);
            methodPhoneButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "PHONE".equals(selectedMethod) ? yellow : gray));
            methodEmailButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "EMAIL".equals(selectedMethod) ? yellow : gray));
        }
    }

    static class OtpVH extends RecyclerView.ViewHolder {
        final TextView otpIntro;
        final LinearLayout formContainer;
        final EditText codeInput;
        final Button verifyButton;
        final TextView resendButton;
        final TextView errorText;
        final TextView devCodeText;

        OtpVH(View itemView) {
            super(itemView);
            otpIntro = itemView.findViewById(R.id.otpIntro);
            formContainer = itemView.findViewById(R.id.otpFormContainer);
            codeInput = itemView.findViewById(R.id.codeInput);
            verifyButton = itemView.findViewById(R.id.verifyButton);
            resendButton = itemView.findViewById(R.id.resendButton);
            errorText = itemView.findViewById(R.id.errorText);
            devCodeText = itemView.findViewById(R.id.devCodeText);
        }

        void bind(ChatMessage msg, Listener listener) {
            String destination = "EMAIL".equals(msg.otpMethod) ? msg.otpEmail : msg.otpPhone;
            String via = "EMAIL".equals(msg.otpMethod) ? "email" : "phone";
            otpIntro.setText("We sent a 6-digit code to your " + via + ": " + destination + " \uD83D\uDC47");
            formContainer.setVisibility(msg.otpActive ? View.VISIBLE : View.GONE);

            if (msg.otpError != null && !msg.otpError.isEmpty()) {
                errorText.setText(msg.otpError);
                errorText.setVisibility(View.VISIBLE);
            } else {
                errorText.setVisibility(View.GONE);
            }

            if (msg.otpDevCode != null && !msg.otpDevCode.isEmpty()) {
                devCodeText.setText("Demo mode (no real SMS sent) - your code is " + msg.otpDevCode);
                devCodeText.setVisibility(View.VISIBLE);
            } else {
                devCodeText.setVisibility(View.GONE);
            }

            verifyButton.setOnClickListener(v -> {
                String code = codeInput.getText().toString().trim();
                if (!code.isEmpty()) listener.onOtpVerify(msg.otpPhone, code);
            });
            resendButton.setOnClickListener(v -> listener.onOtpResend(msg.otpPhone));
        }
    }
}
