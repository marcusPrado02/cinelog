package com.cine.cinelog.core.application.ports.out;

import java.util.Collection;

/**
 * Porta de saída para gerenciar o relacionamento entre Media e Genre
 * na tabela de junção media_genres.
 *
 * Mantém a camada de aplicação desacoplada de JDBC e SQL.
 */
public interface MediaGenreLinkPort {

    /**
     * Remove todos os vínculos de gênero de uma mídia e insere os gêneros
     * informados.
     *
     * @param mediaId  ID da mídia
     * @param genreIds IDs dos gêneros que devem permanecer vinculados
     */
    void replaceGenres(long mediaId, Collection<Long> genreIds);

    /**
     * Cria um vínculo entre mídia e gênero.
     *
     * @param mediaId ID da mídia
     * @param genreId ID do gênero
     */
    void link(long mediaId, long genreId);

    /**
     * Remove um vínculo entre mídia e gênero.
     *
     * @param mediaId ID da mídia
     * @param genreId ID do gênero
     */
    void unlink(long mediaId, long genreId);
}
