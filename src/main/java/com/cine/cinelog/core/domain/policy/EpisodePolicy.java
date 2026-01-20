package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Episode;
/**
 * Política de domínio para gerenciamento de episode.
 * Define as regras e validações relacionadas a episode.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em Episode.</p>
 * 
 * @since 1.0
 * @see Episode
 */

public interface EpisodePolicy {

    void validateCreate(Episode episode);

    void validateUpdate(Episode episode);
}