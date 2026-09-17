package com.zigi.ussd.controller;

import com.zigi.ussd.model.MenuItem;
import java.math.BigDecimal;

public class MenuItemDto {
    public Long id;
    public Long parentId;
    public String groupName;
    public String label;
    public String icon;
    public String responseText;
    public String actionType;
    public String balanceType;
    public BigDecimal price;
    public String creditType;
    public BigDecimal creditAmount;
    public Integer sortOrder;
    public boolean hasChildren;

    public static MenuItemDto from(MenuItem item, boolean hasChildren) {
        MenuItemDto dto = new MenuItemDto();
        dto.id = item.getId();
        dto.parentId = item.getParentId();
        dto.groupName = item.getGroupName();
        dto.label = item.getLabel();
        dto.icon = item.getIcon();
        dto.responseText = item.getResponseText();
        dto.actionType = item.getActionType();
        dto.balanceType = item.getBalanceType();
        dto.price = item.getPrice();
        dto.creditType = item.getCreditType();
        dto.creditAmount = item.getCreditAmount();
        dto.sortOrder = item.getSortOrder();
        dto.hasChildren = hasChildren;
        return dto;
    }
}
