package com.accommodationfinder.repository;

import com.accommodationfinder.model.TokenType;
import com.accommodationfinder.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    @Query("select t from UserToken t join fetch t.user where t.tokenHash = :hash and t.type = :type")
    Optional<UserToken> findByHashAndType(@Param("hash") String hash, @Param("type") TokenType type);

    /** Issuing a new token retires the old ones, so only the latest email's link works. */
    @Modifying
    @Query("update UserToken t set t.usedAt = :now "
            + "where t.user.id = :userId and t.type = :type and t.usedAt is null")
    int retireActive(@Param("userId") Long userId, @Param("type") TokenType type, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from UserToken t where t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
