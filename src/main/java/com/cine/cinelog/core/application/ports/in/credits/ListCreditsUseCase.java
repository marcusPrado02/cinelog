package com.cine.cinelog.core.application.ports.in.credits;

import com.cine.cinelog.core.domain.model.Credit;
import java.util.List;

/**
 * Caso de uso para listagem de créditos.
 * Define a operação de recuperação de todos os créditos disponíveis no sistema.
 * Retorna uma lista de objetos Credit.
 */
public interface ListCreditsUseCase {
    /**
     * Recupera todos os créditos existentes do sistema.
     *
     * @return Uma lista de créditos disponíveis.
     */
    List<Credit> execute();
}
