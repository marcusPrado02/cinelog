package com.cine.cinelog.shared.observability.aop;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Measured {

    /**
     * Nome base da métrica. Se vazio, será usado o default do aspecto.
     * Ex: "cinelog.usecase.sync".
     */
    String value() default "";

    /**
     * Permite desligar medição para um método/classe específica.
     */
    boolean enabled() default true;
}
