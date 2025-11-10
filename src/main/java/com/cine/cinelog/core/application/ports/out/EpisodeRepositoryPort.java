package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Episode;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de persistência relacionadas a episódios.
 * Define as operações CRUD básicas para gerenciar entidades Episode.
 */
public interface EpisodeRepositoryPort {
    /**
     * Salva um episódio no repositório.
     *
     * @param episode O objeto Episode a ser salvo.
     * @return O episódio salvo.
     */
    Episode save(Episode episode);

    /**
     * Recupera um episódio existente do repositório.
     *
     * @param id O ID do episódio a ser recuperado.
     * @return Um Optional contendo o episódio encontrado, ou vazio se não
     *         encontrado.
     */
    Optional<Episode> findById(Long id);

    /**
     * Recupera todos os episódios existentes do repositório.
     *
     * @return Uma lista de episódios encontrados.
     */
    List<Episode> findAll();

    /**
     * Remove um episódio existente do repositório.
     *
     * @param id O ID do episódio a ser removido.
     */
    void deleteById(Long id);
}