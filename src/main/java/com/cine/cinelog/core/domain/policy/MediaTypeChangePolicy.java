package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Media;
/**
 * Política de domínio para gerenciamento de mediatypechange.
 * Define as regras e validações relacionadas a mediatypechange.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em MediaTypeChange.</p>
 * 
 * @since 1.0
 * @see MediaTypeChange
 */

public interface MediaTypeChangePolicy {
    void validate(Media current, Media updated);
}