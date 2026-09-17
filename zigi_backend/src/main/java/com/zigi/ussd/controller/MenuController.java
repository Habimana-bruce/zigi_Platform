package com.zigi.ussd.controller;

import com.zigi.ussd.model.MenuItem;
import com.zigi.ussd.repository.MenuItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuItemRepository repository;

    public MenuController(MenuItemRepository repository) {
        this.repository = repository;
    }

    private MenuItemDto toDto(MenuItem item) {
        boolean hasChildren = repository.existsByParentIdAndIsDeletedFalse(item.getId());
        return MenuItemDto.from(item, hasChildren);
    }

    @GetMapping("/roots/{group}")
    public List<MenuItemDto> getRoots(@PathVariable("group") String group) {
        return repository
                .findByGroupNameAndParentIdIsNullAndIsDeletedFalseOrderBySortOrderAsc(group.toUpperCase())
                .stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}/children")
    public List<MenuItemDto> getChildren(@PathVariable Long id) {
        return repository.findByParentIdAndIsDeletedFalseOrderBySortOrderAsc(id)
                .stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemDto> getOne(@PathVariable Long id) {
        return repository.findById(id)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .map(i -> ResponseEntity.ok(toDto(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/deleted")
    public List<MenuItemDto> getDeleted() {
        return repository.findByIsDeletedTrueOrderByIdDesc().stream().map(this::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<MenuItemDto> create(@RequestBody MenuItem item) {
        item.setId(null);
        item.setIsDeleted(false);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        if (item.getGroupName() != null) item.setGroupName(item.getGroupName().toUpperCase());
        if (item.getActionType() == null) item.setActionType("TEXT");
        MenuItem saved = repository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemDto> update(@PathVariable Long id, @RequestBody MenuItem updated) {
        return repository.findById(id).map(existing -> {
            existing.setLabel(updated.getLabel());
            existing.setIcon(updated.getIcon());
            existing.setResponseText(updated.getResponseText());
            existing.setActionType(updated.getActionType() != null ? updated.getActionType() : "TEXT");
            existing.setBalanceType(updated.getBalanceType());
            existing.setPrice(updated.getPrice());
            existing.setCreditType(updated.getCreditType());
            existing.setCreditAmount(updated.getCreditAmount());
            existing.setSortOrder(updated.getSortOrder());
            if (updated.getParentId() != null) existing.setParentId(updated.getParentId());
            if (updated.getGroupName() != null) existing.setGroupName(updated.getGroupName().toUpperCase());
            if (updated.getIsDeleted() != null) existing.setIsDeleted(updated.getIsDeleted());
            existing.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(toDto(repository.save(existing)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/soft-delete/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        return repository.findById(id).map(existing -> {
            existing.setIsDeleted(true);
            existing.setUpdatedAt(LocalDateTime.now());
            repository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Menu item " + id + " soft-deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/restore/{id}")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        return repository.findById(id).map(existing -> {
            existing.setIsDeleted(false);
            existing.setUpdatedAt(LocalDateTime.now());
            repository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Menu item " + id + " restored"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> hardDelete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Menu item " + id + " permanently deleted"));
    }
}
