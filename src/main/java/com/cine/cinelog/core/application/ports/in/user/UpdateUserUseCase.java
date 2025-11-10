package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;

/**
 * Caso de uso para atualização de usuários.
 * Define a operação de modificação de um usuário existente no sistema,
 * identificando-o pelo seu ID e recebendo um objeto User com os
 * dados atualizados.
 */
public interface UpdateUserUseCase {
    /**
     * Atualiza um usuário existente no sistema.
     *
     * @param id   O ID do usuário a ser atualizado.
     * @param user O objeto User com os novos dados do usuário.
     * @return O usuário atualizado.
     */
    User execute(Long id, User user);
}