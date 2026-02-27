package com.cine.cinelog.features.auth.persistence.repository;

import com.cine.cinelog.features.auth.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * A07:2025 — Repository de Refresh Tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * Revoga TODOS os tokens de uma família (token replay detection).
     * Quando um token já revogado é reutilizado, toda a família é comprometida.
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.revokedAt = :now "
            + "WHERE rt.tokenFamily = :family AND rt.revoked = false")
    int revokeAllByFamily(@Param("family") String tokenFamily, @Param("now") Instant now);

    /**
     * Revoga todos os tokens de um usuário (logout completo / trocar senha).
     */
    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.revokedAt = :now "
            + "WHERE rt.userId = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Remove tokens expirados (housekeeping).
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
