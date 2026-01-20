package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Season;
/**
 * Política de domínio para gerenciamento de season.
 * Define as regras e validações relacionadas a season.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em Season.</p>
 * 
 * @since 1.0
 * @see Season
 */

public interface SeasonPolicy {

    void validateCreate(Season season);

    void validateUpdate(Season season);
}
