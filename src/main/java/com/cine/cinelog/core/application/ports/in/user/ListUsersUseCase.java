package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;
import java.util.List;

/**
 * Caso de uso para listagem de usuários.
 * Define a operação de recuperação de todos os usuários
 * disponíveis no sistema.
 * Retorna uma lista de objetos User.
 */
public interface ListUsersUseCase {
    /**
     * Recupera todos os usuários existentes do sistema.
     *
     * @return Uma lista de usuários disponíveis.
     */
    List<User> execute();
}