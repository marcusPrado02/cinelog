package com.cine.cinelog.core.application.ports.in.user;

/**
 * Caso de uso para exclusão de usuários.
 * Define a operação de remoção de um usuário existente no sistema,
 * identificando-o pelo seu ID.
 */
public interface DeleteUserUseCase {
    /**
     * Remove um usuário existente do sistema.
     *
     * @param id O ID do usuário a ser removido.
     */
    void execute(Long id);
}
