package com.cine.cinelog.shared.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Serviço centralizado para gerenciamento de métricas de negócio customizadas.
 * 
 * <p>
 * Fornece métodos convenientes para registrar contadores, timers e gauges
 * relacionados a operações de negócio do CineLog.
 * </p>
 * 
 * <p>
 * Todas as métricas seguem o padrão de nomenclatura:
 * cinelog.business.{dominio}.{operacao}
 * </p>
 * 
 * @since 1.0
 */
@Service
@Slf4j
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ==================== MEDIA METRICS ====================

    /**
     * Incrementa contador de mídias criadas.
     * 
     * @param mediaType MOVIE ou SERIES
     */
    public void incrementMediaCreated(String mediaType) {
        counter("cinelog.business.media.created",
                Tag.of("type", mediaType))
                .increment();
        log.debug("Métrica incrementada: media.created [type={}]", mediaType);
    }

    /**
     * Incrementa contador de mídias atualizadas.
     */
    public void incrementMediaUpdated(String mediaType) {
        counter("cinelog.business.media.updated",
                Tag.of("type", mediaType))
                .increment();
    }

    /**
     * Incrementa contador de mídias deletadas.
     */
    public void incrementMediaDeleted(String mediaType) {
        counter("cinelog.business.media.deleted",
                Tag.of("type", mediaType))
                .increment();
    }

    /**
     * Incrementa contador de buscas de mídia.
     */
    public void incrementMediaSearch() {
        counter("cinelog.business.media.searched").increment();
    }

    // ==================== USER METRICS ====================

    /**
     * Incrementa contador de usuários registrados.
     */
    public void incrementUserRegistered() {
        counter("cinelog.business.user.registered").increment();
        log.info("Métrica: Novo usuário registrado");
    }

    /**
     * Incrementa contador de logins.
     * 
     * @param success true se login bem-sucedido, false caso contrário
     */
    public void incrementLogin(boolean success) {
        counter("cinelog.business.auth.login",
                Tag.of("success", String.valueOf(success)))
                .increment();
        log.debug("Métrica incrementada: auth.login [success={}]", success);
    }

    /**
     * Incrementa contador de logins falhados.
     */
    public void incrementLoginFailed() {
        counter("cinelog.business.auth.login.failed").increment();
        log.warn("Métrica: Login falhado");
    }

    /**
     * Incrementa contador de refresh tokens.
     */
    public void incrementTokenRefresh() {
        counter("cinelog.business.auth.token.refresh").increment();
    }

    // ==================== WATCH ENTRY METRICS ====================

    /**
     * Incrementa contador de registros de visualização.
     * 
     * @param mediaType MOVIE ou SERIES
     */
    public void incrementWatchEntryCreated(String mediaType) {
        counter("cinelog.business.watchentry.created",
                Tag.of("media_type", mediaType))
                .increment();
        log.debug("Métrica: Watch entry criado [type={}]", mediaType);
    }

    /**
     * Incrementa contador de avaliações registradas.
     * 
     * @param rating nota de 1 a 5
     */
    public void incrementRatingGiven(int rating) {
        counter("cinelog.business.rating.given",
                Tag.of("rating", String.valueOf(rating)))
                .increment();
    }

    // ==================== WATCHLIST METRICS ====================

    /**
     * Incrementa contador de adições à watchlist.
     */
    public void incrementWatchlistAdd(String mediaType) {
        counter("cinelog.business.watchlist.added",
                Tag.of("media_type", mediaType))
                .increment();
    }

    /**
     * Incrementa contador de remoções da watchlist.
     */
    public void incrementWatchlistRemove() {
        counter("cinelog.business.watchlist.removed").increment();
    }

    // ==================== GENRE METRICS ====================

    /**
     * Incrementa contador de gêneros criados.
     */
    public void incrementGenreCreated() {
        counter("cinelog.business.genre.created").increment();
    }

    /**
     * Incrementa contador de gêneros atualizados.
     */
    public void incrementGenreUpdated() {
        counter("cinelog.business.genre.updated").increment();
    }

    /**
     * Incrementa contador de gêneros deletados.
     */
    public void incrementGenreDeleted() {
        counter("cinelog.business.genre.deleted").increment();
    }

    // ==================== SEASON & EPISODE METRICS ====================

    /**
     * Incrementa contador de temporadas criadas.
     */
    public void incrementSeasonCreated() {
        counter("cinelog.business.season.created").increment();
    }

    /**
     * Incrementa contador de episódios criados.
     */
    public void incrementEpisodeCreated() {
        counter("cinelog.business.episode.created").increment();
    }

    // ==================== CREDIT METRICS ====================

    /**
     * Incrementa contador de créditos criados.
     * 
     * @param role ACTOR, DIRECTOR, PRODUCER, etc.
     */
    public void incrementCreditCreated(String role) {
        counter("cinelog.business.credit.created",
                Tag.of("role", role))
                .increment();
    }

    // ==================== PERSON METRICS ====================

    /**
     * Incrementa contador de pessoas criadas.
     */
    public void incrementPersonCreated() {
        counter("cinelog.business.person.created").increment();
    }

    // ==================== INTEGRATION METRICS ====================

    /**
     * Incrementa contador de chamadas à API TMDB.
     * 
     * @param endpoint endpoint chamado
     * @param success  true se sucesso, false se erro
     */
    public void incrementTmdbApiCall(String endpoint, boolean success) {
        counter("cinelog.integration.tmdb.calls",
                Tag.of("endpoint", endpoint),
                Tag.of("success", String.valueOf(success)))
                .increment();
    }

    /**
     * Registra tempo de resposta de integrações externas.
     * 
     * @param integration nome da integração (ex: tmdb, email)
     * @param durationMs  duração em milissegundos
     */
    public void recordIntegrationDuration(String integration, long durationMs) {
        timer("cinelog.integration.duration",
                Tag.of("integration", integration))
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // ==================== ERROR METRICS ====================

    /**
     * Incrementa contador de erros de validação.
     * 
     * @param entity entidade que falhou na validação
     */
    public void incrementValidationError(String entity) {
        counter("cinelog.business.validation.error",
                Tag.of("entity", entity))
                .increment();
        log.debug("Métrica: Erro de validação [entity={}]", entity);
    }

    /**
     * Incrementa contador de erros de domínio.
     * 
     * @param exceptionType tipo da exceção de domínio
     */
    public void incrementDomainError(String exceptionType) {
        counter("cinelog.business.domain.error",
                Tag.of("exception", exceptionType))
                .increment();
        log.warn("Métrica: Erro de domínio [exception={}]", exceptionType);
    }

    /**
     * Incrementa contador de recursos não encontrados (404).
     * 
     * @param resourceType tipo do recurso (media, user, etc.)
     */
    public void incrementResourceNotFound(String resourceType) {
        counter("cinelog.business.notfound",
                Tag.of("resource", resourceType))
                .increment();
    }

    // ==================== CACHE METRICS ====================

    /**
     * Incrementa contador de cache hits.
     * 
     * @param cacheName nome do cache
     */
    public void incrementCacheHit(String cacheName) {
        counter("cinelog.cache.hit",
                Tag.of("cache", cacheName))
                .increment();
    }

    /**
     * Incrementa contador de cache misses.
     * 
     * @param cacheName nome do cache
     */
    public void incrementCacheMiss(String cacheName) {
        counter("cinelog.cache.miss",
                Tag.of("cache", cacheName))
                .increment();
    }

    // ==================== BATCH METRICS ====================

    /**
     * Incrementa contador de jobs batch executados.
     * 
     * @param jobName nome do job
     * @param success true se sucesso
     */
    public void incrementBatchJobExecuted(String jobName, boolean success) {
        counter("cinelog.batch.job.executed",
                Tag.of("job", jobName),
                Tag.of("success", String.valueOf(success)))
                .increment();
    }

    /**
     * Registra número de itens processados em batch.
     * 
     * @param jobName        nome do job
     * @param itemsProcessed quantidade processada
     */
    public void recordBatchItemsProcessed(String jobName, int itemsProcessed) {
        counter("cinelog.batch.items.processed",
                Tag.of("job", jobName))
                .increment(itemsProcessed);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Cria ou recupera um contador.
     */
    private Counter counter(String name, Tag... tags) {
        return Counter.builder(name)
                .description("Business metric for " + name)
                .tags(Arrays.asList(tags))
                .register(meterRegistry);
    }

    /**
     * Cria ou recupera um timer.
     */
    private Timer timer(String name, Tag... tags) {
        return Timer.builder(name)
                .description("Timer metric for " + name)
                .tags(Arrays.asList(tags))
                .register(meterRegistry);
    }
}
