package com.zigi.ussd.repository;

import com.zigi.ussd.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByGroupNameAndParentIdIsNullAndIsDeletedFalseOrderBySortOrderAsc(String groupName);

    List<MenuItem> findByParentIdAndIsDeletedFalseOrderBySortOrderAsc(Long parentId);

    List<MenuItem> findByIsDeletedTrueOrderByIdDesc();

    boolean existsByParentIdAndIsDeletedFalse(Long parentId);
}
