package com.accommodationfinder.repository;

import com.accommodationfinder.model.Report;
import com.accommodationfinder.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    long countByRoomId(Long roomId);

    List<Report> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    boolean existsByReporterIdAndRoomId(Long reporterId, Long roomId);
}
