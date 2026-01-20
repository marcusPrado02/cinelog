package com.cine.cinelog.core.domain.policy;

import com.cine.cinelog.core.domain.model.Media;

/**
 * Regras de negócio para deleção de mídias.
 *
 * MD1: Não pode deletar mídia com seasons associadas.
 * MD2: Não pode deletar mídia com episodes associados.
 * MD3: Não pode deletar mídia com watch entries.
 * MD4: Não pode deletar mídia presente em alguma watchlist.
 * MD5: Não pode deletar mídia vinculada a provedor externo (tmdbId).
 */
public interface MediaDeletionPolicy {

    void validateDelete(Media media);
}
