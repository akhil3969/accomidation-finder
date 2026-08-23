package com.accommodationfinder.repository;

import com.accommodationfinder.model.ChecklistProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistProgressRepository extends JpaRepository<ChecklistProgress, Long> {

    List<ChecklistProgress> findByUserId(Long userId);

    Optional<ChecklistProgress> findByUserIdAndItemId(Long userId, Long itemId);

    long countByUserIdAndDoneTrue(Long userId);
}
