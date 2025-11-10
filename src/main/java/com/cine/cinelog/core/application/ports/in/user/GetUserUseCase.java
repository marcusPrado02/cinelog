package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;

/**
 * Caso de uso para obtenção de usuários.
 * Define a operação de recuperação de um usuário existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface GetUserUseCase {
    /**
     * Recupera um usuário existente do sistema.
     *
     * @param id O ID do usuário a ser recuperado.
     * @return O usuário correspondente ao ID fornecido.
     */
    User execute(Long id);
}
