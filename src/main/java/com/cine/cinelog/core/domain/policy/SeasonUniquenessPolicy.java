package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Season;
/**
 * Política de domínio para gerenciamento de seasonuniqueness.
 * Define as regras e validações relacionadas a seasonuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em SeasonUniqueness.</p>
 * 
 * @since 1.0
 * @see SeasonUniqueness
 */

public interface SeasonUniquenessPolicy {

    void validateCreate(Season season);
}
