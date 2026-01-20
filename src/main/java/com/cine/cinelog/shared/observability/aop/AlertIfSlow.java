package com.cine.cinelog.shared.observability.aop;

import java.lang.annotation.*;

/**
 * Anotação para marcar métodos que devem ser monitorados quanto à lentidão.
 *
 * Se o tempo de execução exceder o thresholdMs, será emitido:
 * - log WARN
 * - incremento na métrica configurada (default: cinelog.slow_method)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AlertIfSlow {

    /**
     * Limite de tempo em milissegundos a partir do qual será gerado um alerta.
     */
    long thresholdMs() default 500L;

    /**
     * Nome da métrica a ser registrada quando o limite for excedido.
     */
    String metricName() default "cinelog.slow_method";
}
