package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Credit;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de persistência relacionadas a créditos.
 * Define as operações CRUD básicas para gerenciar entidades Credit.
 */
public interface CreditRepositoryPort {

    /**
     * Salva um crédito no repositório.
     *
     * @param credit O objeto Credit a ser salvo.
     * @return O crédito salvo.
     */
    Credit save(Credit credit);

    /**
     * Recupera um crédito existente do repositório.
     *
     * @param id O ID do crédito a ser recuperado.
     * @return Um Optional contendo o crédito encontrado, ou vazio se não
     *         encontrado.
     */
    Optional<Credit> findById(Long id);

    /**
     * Recupera todos os créditos existentes do repositório.
     *
     * @return Uma lista de créditos encontrados.
     */
    List<Credit> findAll();

    /**
     * Remove um crédito existente do repositório.
     *
     * @param id O ID do crédito a ser removido.
     */
    void deleteById(Long id);
}
