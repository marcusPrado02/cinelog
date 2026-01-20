package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.WatchEntry;
/**
 * Política de domínio para gerenciamento de watchentryuniqueness.
 * Define as regras e validações relacionadas a watchentryuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em WatchEntryUniqueness.</p>
 * 
 * @since 1.0
 * @see WatchEntryUniqueness
 */

public interface WatchEntryUniquenessPolicy {
    void validate(WatchEntry entry);
}