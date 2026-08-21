package com.accommodationfinder.service;

import com.accommodationfinder.dto.AdminDtos.*;
import com.accommodationfinder.model.BookingStatus;
import com.accommodationfinder.model.ReportStatus;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.VerificationStatus;
import com.accommodationfinder.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the database into the numbers and series the admin dashboard draws.
 *
 * <p>The date-bucketed queries return only days that had activity. Charts need every day in
 * the window, including the empty ones - a gap and a zero look very different on a line -
 * so the series are padded here rather than in the browser.
 */
@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final ReportRepository reportRepository;

    public AnalyticsService(UserRepository userRepository,
                            RoomRepository roomRepository,
                            BookingRepository bookingRepository,
                            ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsBundle bundle(int days) {
        int window = Math.min(Math.max(days, 7), 365);
        LocalDateTime since = LocalDate.now().minusDays(window - 1L).atStartOfDay();

        return new AnalyticsBundle(
                stats(),
                padSeries(userRepository.countPerDaySince(since), window),
                padSeries(roomRepository.countPerDaySince(since), window),
                padSeries(bookingRepository.countPerDaySince(since), window),
                toCategories(roomRepository.countByCity(), 8),
                bookingsByStatus(),
                toCategories(userRepository.countByNationality(), 10),
                listingsByType(),
                window);
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        return new DashboardStats(
                userRepository.count(),
                userRepository.countByRole(Role.TENANT),
                userRepository.countByRole(Role.LANDLORD),
                userRepository.countByCreatedAtAfter(weekAgo),
                roomRepository.count(),
                roomRepository.countByAvailableTrue(),
                roomRepository.countByVerifiedTrue(),
                roomRepository.countByHiddenTrue(),
                roomRepository.countByCreatedAtAfter(weekAgo),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                bookingRepository.countByRequestedAtAfter(weekAgo),
                reportRepository.countByStatus(ReportStatus.OPEN),
                userRepository.countByVerificationStatus(VerificationStatus.PENDING),
                roomRepository.findTop20ByRiskScoreGreaterThanOrderByRiskScoreDesc(50).size());
    }

    // ------------------------------------------------------------------ series

    /**
     * Fills in the days the query skipped. A missing Tuesday means nobody signed up on
     * Tuesday, which is a zero, not an absence of data.
     */
    private List<TimePoint> padSeries(List<Object[]> rows, int days) {
        Map<String, Long> byDate = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) {
                continue;
            }
            byDate.put(String.valueOf(row[0]).substring(0, 10), ((Number) row[1]).longValue());
        }

        List<TimePoint> series = new ArrayList<>(days);
        LocalDate cursor = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            String key = cursor.toString();
            series.add(new TimePoint(key, byDate.getOrDefault(key, 0L)));
            cursor = cursor.plusDays(1);
        }
        return series;
    }

    private List<CategoryCount> toCategories(List<Object[]> rows, int limit) {
        List<CategoryCount> categories = new ArrayList<>();
        long othersTotal = 0;
        int index = 0;

        for (Object[] row : rows) {
            String label = row[0] == null ? "Unknown" : String.valueOf(row[0]);
            long value = ((Number) row[1]).longValue();
            if (index < limit) {
                categories.add(new CategoryCount(label, value));
            } else {
                othersTotal += value;
            }
            index++;
        }
        if (othersTotal > 0) {
            categories.add(new CategoryCount("Other", othersTotal));
        }
        return categories;
    }

    private List<CategoryCount> bookingsByStatus() {
        List<CategoryCount> categories = new ArrayList<>();
        for (Object[] row : bookingRepository.countGroupedByStatus()) {
            categories.add(new CategoryCount(
                    prettify(String.valueOf(row[0])), ((Number) row[1]).longValue()));
        }
        return categories;
    }

    private List<CategoryCount> listingsByType() {
        Map<String, Long> counts = new LinkedHashMap<>();
        roomRepository.findAll().forEach(room -> {
            if (room.isHidden() || room.getRoomType() == null) {
                return;
            }
            counts.merge(prettify(room.getRoomType().name()), 1L, Long::sum);
        });
        return counts.entrySet().stream()
                .map(entry -> new CategoryCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String prettify(String enumName) {
        String lower = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
