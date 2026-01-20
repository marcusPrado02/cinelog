package com.cine.cinelog.shared.observability.aop;

import java.lang.annotation.*;

/**
 * Anotação para marcar ações de negócio que devem gerar trilha de auditoria.
 *
 * Pode ser usada em métodos de casos de uso ou serviços de aplicação.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditableAction {

    /**
     * Módulo de negócio (ex: MEDIA, WATCHLIST, USER).
     */
    String module();

    /**
     * Ação de negócio (ex: CREATE, UPDATE, SYNC_FROM_TMDB).
     */
    String action();

    /**
     * Descrição opcional mais detalhada da ação.
     */
    String description() default "";
}
