package com.azhost.github.repository;

import com.azhost.github.entity.OAuthStateTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthStateTokenRepository extends JpaRepository<OAuthStateTokenEntity, String> {

    Optional<OAuthStateTokenEntity> findByStateTokenAndUserId(String stateToken, UUID userId);

    @Modifying
    @Query("DELETE FROM OAuthStateTokenEntity t WHERE t.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") ZonedDateTime now);
}
