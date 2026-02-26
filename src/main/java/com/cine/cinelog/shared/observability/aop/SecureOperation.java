package com.cine.cinelog.shared.observability.aop;

import java.lang.annotation.*;

/**
 * Anotação para marcar operações sensíveis que devem ser monitoradas e
 * protegidas do ponto de vista de segurança.
 *
 * Por padrão, {@code enforce=true}, portanto a operação é validada contra as
 * authorities do usuário autenticado.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SecureOperation {

    /**
     * Código da permissão necessária (mapeada em GrantedAuthority).
     * Ex: "MEDIA_ADMIN", "WATCHLIST_MANAGE".
     */
    String value()

    default "";

    /**
     * Módulo de negócio relacionado à operação (ex: MEDIA, USER, WATCHLIST).
     */
    String module()

    default "";

    /**
     * Quando true, fará uma checagem simples de permissão com base nas authorities
     * do usuário autenticado e lançará AccessDeniedException se não autorizado.
     */
    boolean enforce() default true;
}
