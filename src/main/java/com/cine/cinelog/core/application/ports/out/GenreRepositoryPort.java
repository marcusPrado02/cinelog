package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Genre;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de repositório relacionadas a gêneros.
 * Define as operações CRUD básicas para gerenciar entidades Genre.
 */
public interface GenreRepositoryPort {
    /**
     * Salva um gênero no repositório.
     *
     * @param genre O objeto Genre a ser salvo.
     * @return O gênero salvo.
     */
    Genre save(Genre genre);

    /**
     * Recupera um gênero existente do repositório.
     *
     * @param id O ID do gênero a ser recuperado.
     * @return Um Optional contendo o gênero encontrado, ou vazio se não
     *         encontrado.
     */
    Optional<Genre> findById(Long id);

    /**
     * Recupera todos os gêneros existentes do repositório.
     *
     * @return Uma lista de gêneros encontrados.
     */
    List<Genre> findAll();

    /**
     * Remove um gênero existente do repositório.
     *
     * @param id O ID do gênero a ser removido.
     */
    void deleteById(Long id);
}