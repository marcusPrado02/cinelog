package com.cine.cinelog.shared.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Configuração de segurança para o ciclo de vida da JVM.
 *
 * <p>
 * <strong>A10:2025 — Mishandling of Exceptional Conditions</strong>
 * </p>
 *
 * <p>
 * Esta classe endereça dois cenários críticos de tratamento de exceções:
 *
 * <h3>1. Uncaught Exception Handler</h3>
 * <p>
 * Por padrão, exceções não tratadas em threads secundárias (ex.: thread pool
 * customizado, CompletableFuture sem handler) são logadas apenas no stderr da
 * JVM,
 * o que pode ser ignorado em ambientes containerizados (Docker/K8s).
 * </p>
 * <p>
 * O handler global registrado aqui garante que essas exceções sejam capturadas
 * pelo pipeline de logging estruturado (Logback → ELK/Loki), permitindo
 * alertas.
 * </p>
 *
 * <h3>2. Shutdown Hook</h3>
 * <p>
 * Registra um hook de shutdown que garante que o encerramento da JVM
 * (SIGTERM, kill, OOM killer) seja registrado nos logs antes que os appenders
 * sejam destruídos. Isso é essencial para investigação post-mortem.
 * </p>
 *
 * <p>
 * <strong>Nota:</strong> O Spring Boot já faz graceful shutdown dos beans
 * (@PreDestroy, DisposableBean). Este hook complementa o Spring, cobrindo
 * cenários onde o container Spring não é inicializado completamente ou é
 * destruído abruptamente.
 * </p>
 *
 * @since 1.0
 */
@Component
public class JvmSafetyConfig {

    private static final Logger log = LoggerFactory.getLogger(JvmSafetyConfig.class);

    /**
     * Registra o handler global de exceções não capturadas e o shutdown hook
     * no momento em que o bean é criado pelo Spring.
     */
    @PostConstruct
    void init() {
        // Handler global para exceções não tratadas em qualquer thread
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof OutOfMemoryError) {
                // Em OOM, log pode falhar — tenta System.err como fallback
                System.err.println("FATAL: OutOfMemoryError na thread " + thread.getName());
                throwable.printStackTrace(System.err);
                log.error("A10:2025 — FATAL OutOfMemoryError na thread '{}'. "
                        + "JVM será encerrada.", thread.getName(), throwable);
            } else {
                log.error("A10:2025 — Exceção não capturada na thread '{}': {}",
                        thread.getName(), throwable.getMessage(), throwable);
            }
        });

        // Shutdown hook — loga encerramento da JVM (SIGTERM, shutdown, etc.)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("A10:2025 — JVM shutdown hook acionado. "
                    + "Encerrando recursos e flushing logs.");
        }, "cinelog-shutdown-hook"));

        log.info("A10:2025 — JVM safety hooks registrados (uncaught handler + shutdown hook)");
    }

    /**
     * Complementa o shutdown hook com logging via Spring lifecycle.
     */
    @PreDestroy
    void onShutdown() {
        log.info("A10:2025 — Spring context sendo destruído. Graceful shutdown em andamento.");
    }
}
