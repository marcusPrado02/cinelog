package com.cine.cinelog.core.domain.policy;

import java.math.BigDecimal;
import java.time.Instant;

import com.cine.cinelog.core.domain.model.WatchEntry;
/**
 * Política de domínio para gerenciamento de rating.
 * Define as regras e validações relacionadas a rating.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em Rating.</p>
 * 
 * @since 1.0
 * @see Rating
 */

public interface RatingPolicy {
    void validateCanRate(WatchEntry entry, BigDecimal rating, Instant when);
}