package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Media;
/**
 * Política de domínio para gerenciamento de media.
 * Define as regras e validações relacionadas a media.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em Media.</p>
 * 
 * @since 1.0
 * @see Media
 */

public interface MediaPolicy {
    void validateInvariants(Media media);
}