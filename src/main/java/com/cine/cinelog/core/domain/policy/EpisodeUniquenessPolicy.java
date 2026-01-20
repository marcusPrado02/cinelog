package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Episode;
/**
 * Política de domínio para gerenciamento de episodeuniqueness.
 * Define as regras e validações relacionadas a episodeuniqueness.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em EpisodeUniqueness.</p>
 * 
 * @since 1.0
 * @see EpisodeUniqueness
 */

public interface EpisodeUniquenessPolicy {

    void validateCreate(Episode episode);
}