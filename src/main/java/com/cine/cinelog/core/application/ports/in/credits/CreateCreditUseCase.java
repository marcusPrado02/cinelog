package com.cine.cinelog.core.application.ports.in.credits;

import com.cine.cinelog.core.domain.model.Credit;

/**
 * Caso de uso para criação de créditos.
 * Define a operação de criação de um novo crédito no sistema,
 * recebendo um objeto Credit e retornando o crédito criado.
 */
public interface CreateCreditUseCase {
    Credit execute(Credit credit);
}
