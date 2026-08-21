package com.accommodationfinder.repository;

import com.accommodationfinder.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByActiveTrueOrderBySortOrderAsc();

    List<ChecklistItem> findAllByOrderBySortOrderAsc();

    Optional<ChecklistItem> findByKey(String key);
}
