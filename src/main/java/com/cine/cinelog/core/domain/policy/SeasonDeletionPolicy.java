package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Season;
/**
 * Política de domínio para gerenciamento de seasondeletion.
 * Define as regras e validações relacionadas a seasondeletion.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em SeasonDeletion.</p>
 * 
 * @since 1.0
 * @see SeasonDeletion
 */

public interface SeasonDeletionPolicy {

    void validateDelete(Season season);
}