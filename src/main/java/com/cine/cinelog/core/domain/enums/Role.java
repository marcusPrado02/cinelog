package com.cine.cinelog.core.domain.enums;

/**
 * Papéis que uma pessoa pode ter em relação a uma mídia.
 * 
 * DIRECTOR - Diretor
 * WRITER - Roteirista
 * ACTOR - Ator/Atriz
 * PRODUCER - Produtor/Produtora
 * COMPOSER - Compositor/Compositora
 * 
 * Este enum é usado para categorizar o papel de uma pessoa em uma mídia,
 * influenciando regras de negócio, exibições e filtros de busca.
 */
public enum Role {
    DIRECTOR,
    WRITER,
    ACTOR,
    PRODUCER,
    COMPOSER
}
