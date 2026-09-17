package com.zigi.ussd.service;

import com.zigi.ussd.model.Customer;
import com.zigi.ussd.model.MenuItem;
import com.zigi.ussd.repository.CustomerRepository;
import com.zigi.ussd.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

@Service
public class UssdEngine {

    private final MenuItemRepository menuRepository;
    private final CustomerRepository customerRepository;
    private final PaymentService paymentService;

    public UssdEngine(MenuItemRepository menuRepository, CustomerRepository customerRepository, PaymentService paymentService) {
        this.menuRepository = menuRepository;
        this.customerRepository = customerRepository;
        this.paymentService = paymentService;
    }

    private static class OptionNode {
        String label;
        boolean isGroupLink;
        String groupName;
        MenuItem item;

        static OptionNode ofItem(MenuItem item) {
            OptionNode n = new OptionNode();
            n.label = item.getLabel();
            n.item = item;
            return n;
        }

        static OptionNode ofGroupLink(String label, String groupName) {
            OptionNode n = new OptionNode();
            n.label = label;
            n.isGroupLink = true;
            n.groupName = groupName;
            return n;
        }
    }

    private static class Level {
        String title;
        List<OptionNode> options;
        Level(String title, List<OptionNode> options) { this.title = title; this.options = options; }
    }

    private List<OptionNode> rootOptions() {
        List<OptionNode> nodes = new ArrayList<>();
        for (MenuItem m : menuRepository.findByGroupNameAndParentIdIsNullAndIsDeletedFalseOrderBySortOrderAsc("MAIN")) {
            nodes.add(OptionNode.ofItem(m));
        }
        nodes.add(OptionNode.ofGroupLink("Airtime", "AIRTIME"));
        nodes.add(OptionNode.ofGroupLink("Data", "DATA"));
        nodes.add(OptionNode.ofGroupLink("Assist", "ASSIST"));
        nodes.add(OptionNode.ofGroupLink("Help", "HELP"));
        return nodes;
    }

    private String invalidOptionResponse(Deque<Level> stack) {
        Level current = stack.peek();
        return "CON Invalid option selected.\n" + formatMenu(current.title, current.options, stack.size() > 1);
    }

    private String formatMenu(String title, List<OptionNode> options, boolean canGoBack) {
        StringBuilder sb = new StringBuilder(title).append("\n");
        for (int i = 0; i < options.size(); i++) {
            sb.append(i + 1).append(". ").append(options.get(i).label).append("\n");
        }
        sb.append(canGoBack ? "0. Back" : "0. Exit");
        return sb.toString();
    }

    private String balanceResponse(String balanceType) {

        if ("AIRTIME".equals(balanceType) || "DATA".equals(balanceType)) {
            return "END Your request is being processed. You will receive your balance information via SMS shortly.";
        }
        return "END Your request is being processed. You will receive your balance summary via SMS shortly.";
    }

    public static class UssdStepResult {
        public final String response;
        public final boolean stepAccepted;
        public UssdStepResult(String response, boolean stepAccepted) {
            this.response = response;
            this.stepAccepted = stepAccepted;
        }
    }

    public UssdStepResult processStep(String phoneNumber, String text) {
        String[] steps = text == null || text.isEmpty() ? new String[0] : text.split("\\*");

        Deque<Level> stack = new ArrayDeque<>();
        stack.push(new Level("Welcome to Zigi", rootOptions()));

        if (steps.length == 0) {
            Level root = stack.peek();
            return new UssdStepResult("CON " + formatMenu(root.title, root.options, false), true);
        }

        for (int i = 0; i < steps.length; i++) {
            String step = steps[i].trim();
            boolean isLastStep = (i == steps.length - 1);

            if (step.equals("0")) {
                if (stack.size() > 1) {
                    stack.pop();
                    Level cur = stack.peek();
                    if (isLastStep) return new UssdStepResult("CON " + formatMenu(cur.title, cur.options, stack.size() > 1), true);
                    continue;
                }
                return new UssdStepResult("END Session ended. Goodbye!", true);
            }

            int idx;
            try {
                idx = Integer.parseInt(step);
            } catch (NumberFormatException e) {
                return new UssdStepResult(invalidOptionResponse(stack), false);
            }
            Level current = stack.peek();
            if (idx < 1 || idx > current.options.size()) {
                return new UssdStepResult(invalidOptionResponse(stack), false);
            }
            OptionNode node = current.options.get(idx - 1);

            if (node.isGroupLink) {
                List<MenuItem> roots = menuRepository
                        .findByGroupNameAndParentIdIsNullAndIsDeletedFalseOrderBySortOrderAsc(node.groupName);
                List<OptionNode> nodeOptions = roots.stream().map(OptionNode::ofItem).toList();
                stack.push(new Level(node.label, nodeOptions));
                if (isLastStep) return new UssdStepResult("CON " + formatMenu(node.label, nodeOptions, true), true);
                continue;
            }

            MenuItem item = node.item;

            if ("BALANCE".equals(item.getActionType())) {
                if (!isLastStep) return new UssdStepResult(invalidOptionResponse(stack), false);
                Optional<Customer> c = customerRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber);
                if (c.isEmpty()) return new UssdStepResult("END Your request is being processed. " +
                        "You will receive your balance information via SMS shortly.", true);
                return new UssdStepResult(balanceResponse(item.getBalanceType()), true);
            }

            if ("PAYMENT".equals(item.getActionType())) {
                if (!isLastStep) return new UssdStepResult(invalidOptionResponse(stack), false);
                PaymentService.InitiateResult result = paymentService.initiatePayment(phoneNumber, item.getId(), "MTN", "USSD");
                if (result.error != null) return new UssdStepResult("END " + result.error, true);
                return new UssdStepResult("END Your request is being processed. " +
                        "You will receive an SMS once your purchase of " + item.getLabel() + " is confirmed.", true);
            }

            List<MenuItem> children = menuRepository.findByParentIdAndIsDeletedFalseOrderBySortOrderAsc(item.getId());
            if (!children.isEmpty()) {
                List<OptionNode> childOptions = children.stream().map(OptionNode::ofItem).toList();
                stack.push(new Level(item.getLabel(), childOptions));
                if (isLastStep) return new UssdStepResult("CON " + formatMenu(item.getLabel(), childOptions, true), true);
                continue;
            }

            if (isLastStep) {
                String resp = (item.getResponseText() != null && !item.getResponseText().isBlank())
                        ? item.getResponseText()
                        : (item.getLabel() + " - coming soon.");
                return new UssdStepResult("END " + resp, true);
            }
            return new UssdStepResult(invalidOptionResponse(stack), false);
        }

        return new UssdStepResult("END Session ended.", true);
    }
}
