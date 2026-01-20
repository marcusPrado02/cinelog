package com.cine.cinelog.core.application.ports.in.media;

import com.cine.cinelog.core.domain.model.Media;

/**
 * Caso de uso para sincronizar dados de uma mídia com a API do TMDb (The Movie
 * Database).
 * 
 * <p>
 * Atualiza informações da mídia local buscando dados atualizados do TMDb,
 * incluindo:
 * <ul>
 * <li>Título e título original</li>
 * <li>Descrição (overview)</li>
 * <li>Ano de lançamento</li>
 * <li>Poster e backdrop</li>
 * <li>Rating médio do TMDb</li>
 * <li>Outros metadados relevantes</li>
 * </ul>
 * 
 * <p>
 * A sincronização utiliza o {@code tmdbId} da mídia. Se não estiver definido,
 * pode tentar localizar a mídia no TMDb através de busca por título + ano.
 * 
 * @since 1.0
 * @see Media
 */
public interface SyncMediaFromTmdbUseCase {

    /**
     * Sincroniza os dados de uma mídia com informações do TMDb.
     * 
     * @param mediaId o identificador da mídia local a ser sincronizada
     * @return a mídia atualizada com dados do TMDb
     */
    Media sync(Long mediaId);
}
