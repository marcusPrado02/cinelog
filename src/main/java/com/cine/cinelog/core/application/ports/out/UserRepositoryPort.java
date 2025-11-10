package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.User;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída para operações de repositório de usuários.
 * Define as operações CRUD básicas para gerenciar entidades User.
 */
public interface UserRepositoryPort {
    /**
     * Salva um usuário no repositório.
     *
     * @param user O objeto User a ser salvo.
     * @return O usuário salvo.
     */
    User save(User user);

    /**
     * Recupera um usuário existente do repositório.
     *
     * @param id O ID do usuário a ser recuperado.
     * @return Um Optional contendo o usuário encontrado, ou vazio se não
     *         encontrado.
     */
    Optional<User> findById(Long id);

    /**
     * Recupera todos os usuários existentes do repositório.
     *
     * @return Uma lista de usuários encontrados.
     */
    List<User> findAll();

    /**
     * Remove um usuário existente do repositório.
     *
     * @param id O ID do usuário a ser removido.
     */
    void deleteById(Long id);
}