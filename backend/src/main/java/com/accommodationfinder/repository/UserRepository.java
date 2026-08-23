package com.accommodationfinder.repository;

import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    long countByCreatedAtAfter(LocalDateTime since);

    long countByVerificationStatus(VerificationStatus status);

    List<User> findByVerificationStatusOrderByIdDesc(VerificationStatus status);

    /** Daily signup counts for the admin chart. Each row is [date, count]. */
    @Query("select function('date', u.createdAt), count(u) from User u "
            + "where u.createdAt >= :since "
            + "group by function('date', u.createdAt) "
            + "order by function('date', u.createdAt)")
    List<Object[]> countPerDaySince(@Param("since") LocalDateTime since);

    /** Where our users come from - the chart that makes this app's purpose visible. */
    @Query("select u.nationality, count(u) from User u "
            + "where u.nationality is not null "
            + "group by u.nationality "
            + "order by count(u) desc")
    List<Object[]> countByNationality();
}
