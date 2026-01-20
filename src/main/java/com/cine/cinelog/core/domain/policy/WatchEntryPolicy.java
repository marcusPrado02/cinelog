package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.WatchEntry;
/**
 * Política de domínio para gerenciamento de watchentry.
 * Define as regras e validações relacionadas a watchentry.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em WatchEntry.</p>
 * 
 * @since 1.0
 * @see WatchEntry
 */

public interface WatchEntryPolicy {
    void validateCreate(WatchEntry entry);

    void validateUpdate(WatchEntry entry);
}