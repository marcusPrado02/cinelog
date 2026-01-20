package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Episode;
/**
 * Política de domínio para gerenciamento de episodedeletion.
 * Define as regras e validações relacionadas a episodedeletion.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em EpisodeDeletion.</p>
 * 
 * @since 1.0
 * @see EpisodeDeletion
 */

public interface EpisodeDeletionPolicy {

    void validateDelete(Episode episode);
}