package com.accommodationfinder.repository;

import com.accommodationfinder.model.ChecklistProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistProgressRepository extends JpaRepository<ChecklistProgress, Long> {

    List<ChecklistProgress> findByUserId(Long userId);

    Optional<ChecklistProgress> findByUserIdAndItemId(Long userId, Long itemId);

    long countByUserIdAndDoneTrue(Long userId);

    @Modifying
    @Query("delete from ChecklistProgress p where p.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
