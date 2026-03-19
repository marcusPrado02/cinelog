package com.cine.cinelog.shared.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configura executor de tarefas assíncronas para o serviço de e-mails de
 * relatório.
 *
 * <p>
 * Sem essa configuração, {@code @Async} utiliza
 * {@code SimpleAsyncTaskExecutor},
 * que cria uma nova thread a cada invocação (sem pool, sem fila), podendo levar
 * a explosão de threads sob carga.
 * </p>
 *
 * <p>
 * <b>Configuração do pool:</b>
 * <ul>
 * <li>{@code corePoolSize=2} — 2 threads permanentes para envio de e-mails</li>
 * <li>{@code maxPoolSize=5} — até 5 threads em pico de carga</li>
 * <li>{@code queueCapacity=50} — fila de até 50 tarefas pendentes</li>
 * <li>{@code CallerRunsPolicy} — se fila e pool estão cheios, executa na
 * thread chamadora (backpressure natural, sem perda de e-mails)</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Executor dedicado para envio assíncrono de e-mails de relatório.
     *
     * <p>
     * Referenciar pelo nome: {@code @Async("reportTaskExecutor")}.
     * </p>
     */
    @Bean("reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("report-email-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Executor padrão para tarefas {@code @Async} sem qualificador explícito.
     * Reutiliza o mesmo pool de relatórios para evitar proliferação de pools.
     */
    @Override
    public Executor getAsyncExecutor() {
        return reportTaskExecutor();
    }
}
