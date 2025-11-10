package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;

/**
 * Caso de uso para criação de usuários.
 * Define a operação de inserção de um novo usuário no sistema,
 * recebendo um objeto User com os dados do usuário a ser criado.
 */
public interface CreateUserUseCase {
    /**
     * Cria um novo usuário no sistema.
     *
     * @param user O objeto User com os dados do usuário a ser criado.
     * @return O usuário criado.
     */
    User execute(User user);
}
