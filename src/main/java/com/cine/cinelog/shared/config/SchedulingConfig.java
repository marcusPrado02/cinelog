package com.cine.cinelog.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;

/**
 * Configuração global para tarefas agendadas (@Scheduled).
 *
 * <p>
 * <strong>A10:2025 — Mishandling of Exceptional Conditions</strong>
 * </p>
 *
 * <p>
 * Por padrão, se um método {@code @Scheduled} lança uma exceção não tratada,
 * o Spring loga e <strong>continua</strong> com o scheduler. Porém, contar
 * apenas
 * com isso é arriscado: se o pool de threads estiver configurado incorretamente
 * ou a exceção for um {@link Error}, o scheduler pode morrer silenciosamente.
 * </p>
 *
 * <p>
 * Este {@link ErrorHandler} global oferece uma camada extra de segurança:
 * <ul>
 * <li>Loga a exceção com contexto (thread name)</li>
 * <li>Registra métrica para alertas (via Micrometer)</li>
 * <li>Garante que {@link Error} graves (OOM, StackOverflow) sejam
 * re-lançados</li>
 * </ul>
 *
 * @since 1.0
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    /**
     * TaskScheduler customizado com ErrorHandler global.
     *
     * <p>
     * Todas as tarefas {@code @Scheduled} da aplicação passam por este
     * error handler, o que evita falhas silenciosas mesmo quando o desenvolvedor
     * esquece o try-catch no método agendado.
     * </p>
     */
    @Bean
    public TaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("cinelog-sched-");
        scheduler.setErrorHandler(scheduledTaskErrorHandler());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * ErrorHandler global para tarefas agendadas.
     */
    @Bean
    public ErrorHandler scheduledTaskErrorHandler() {
        return throwable -> {
            String thread = Thread.currentThread().getName();

            if (throwable instanceof Error error) {
                // Errors graves (OOM, StackOverflow) devem subir — não abafar
                log.error("A10:2025 — ERRO FATAL em tarefa agendada [thread={}]. "
                        + "Re-lançando Error: {}", thread, error.getClass().getSimpleName(), error);
                throw error;
            }

            log.error("A10:2025 — Exceção não tratada em tarefa agendada [thread={}]. "
                    + "O scheduler continuará normalmente.", thread, throwable);
        };
    }
}
