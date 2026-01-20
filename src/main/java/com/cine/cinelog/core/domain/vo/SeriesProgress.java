package com.cine.cinelog.core.domain.vo;

import java.time.Duration;
import java.util.Objects;

/**
 * Value Object representando o progresso de visualização de uma série.
 *
 * <p>
 * <strong>Feature:</strong> WatchProgress (PR6 - Fase 5)
 *
 * <p>
 * Encapsula informações de progresso para séries:
 * <ul>
 * <li>Episódio atual (season + episode number)</li>
 * <li>Tempo assistido no episódio atual</li>
 * <li>Percentual de conclusão do episódio</li>
 * </ul>
 *
 * <p>
 * <strong>Imutabilidade:</strong> Value Object é imutável após criação.
 * Qualquer modificação retorna nova instância.
 *
 * <p>
 * <strong>Validações:</strong>
 * <ul>
 * <li>Season number ≥ 1</li>
 * <li>Episode number ≥ 1</li>
 * <li>Watched duration ≥ 0</li>
 * <li>Total duration > 0</li>
 * <li>Watched ≤ Total</li>
 * </ul>
 *
 * @since 1.0 (PR6 - Fase 5)
 */
public final class SeriesProgress {

    private final int currentSeason;
    private final int currentEpisode;
    private final Duration watchedDuration;
    private final Duration totalDuration;

    /**
     * Cria progresso de série com validações.
     *
     * @param currentSeason   temporada atual (≥ 1)
     * @param currentEpisode  episódio atual (≥ 1)
     * @param watchedDuration tempo assistido (≥ 0)
     * @param totalDuration   duração total (> 0)
     * @throws IllegalArgumentException se alguma validação falhar
     */
    public SeriesProgress(int currentSeason, int currentEpisode,
            Duration watchedDuration, Duration totalDuration) {
        // Validações
        if (currentSeason < 1) {
            throw new IllegalArgumentException("Current season must be >= 1, got: " + currentSeason);
        }
        if (currentEpisode < 1) {
            throw new IllegalArgumentException("Current episode must be >= 1, got: " + currentEpisode);
        }
        if (watchedDuration == null || watchedDuration.isNegative()) {
            throw new IllegalArgumentException("Watched duration must be >= 0, got: " + watchedDuration);
        }
        if (totalDuration == null || totalDuration.isZero() || totalDuration.isNegative()) {
            throw new IllegalArgumentException("Total duration must be > 0, got: " + totalDuration);
        }
        if (watchedDuration.compareTo(totalDuration) > 0) {
            throw new IllegalArgumentException(
                    String.format("Watched duration (%s) cannot exceed total duration (%s)",
                            watchedDuration, totalDuration));
        }

        this.currentSeason = currentSeason;
        this.currentEpisode = currentEpisode;
        this.watchedDuration = watchedDuration;
        this.totalDuration = totalDuration;
    }

    /**
     * Cria progresso inicial (S01E01, 0 segundos assistidos).
     *
     * @param episodeDuration duração total do episódio
     * @return nova instância com progresso zerado
     */
    public static SeriesProgress initial(Duration episodeDuration) {
        return new SeriesProgress(1, 1, Duration.ZERO, episodeDuration);
    }

    /**
     * Cria progresso no início de um episódio específico.
     *
     * @param season          temporada
     * @param episode         episódio
     * @param episodeDuration duração do episódio
     * @return nova instância
     */
    public static SeriesProgress at(int season, int episode, Duration episodeDuration) {
        return new SeriesProgress(season, episode, Duration.ZERO, episodeDuration);
    }

    /**
     * Avança para próximo episódio (mesmo season).
     *
     * @param nextEpisodeDuration duração do próximo episódio
     * @return nova instância com episódio incrementado
     */
    public SeriesProgress nextEpisode(Duration nextEpisodeDuration) {
        return new SeriesProgress(
                this.currentSeason,
                this.currentEpisode + 1,
                Duration.ZERO,
                nextEpisodeDuration);
    }

    /**
     * Avança para próxima temporada (episódio 1).
     *
     * @param firstEpisodeDuration duração do primeiro episódio da nova temporada
     * @return nova instância com season incrementado
     */
    public SeriesProgress nextSeason(Duration firstEpisodeDuration) {
        return new SeriesProgress(
                this.currentSeason + 1,
                1,
                Duration.ZERO,
                firstEpisodeDuration);
    }

    /**
     * Atualiza tempo assistido do episódio atual.
     *
     * @param newWatchedDuration novo tempo assistido
     * @return nova instância com tempo atualizado
     */
    public SeriesProgress updateWatchedTime(Duration newWatchedDuration) {
        return new SeriesProgress(
                this.currentSeason,
                this.currentEpisode,
                newWatchedDuration,
                this.totalDuration);
    }

    /**
     * Calcula percentual de conclusão do episódio atual.
     *
     * @return percentual (0.0 - 100.0)
     */
    public double getCompletionPercentage() {
        if (totalDuration.isZero()) {
            return 0.0;
        }
        double watched = watchedDuration.toSeconds();
        double total = totalDuration.toSeconds();
        return Math.min(100.0, (watched / total) * 100.0);
    }

    /**
     * Verifica se episódio atual foi completado (≥ 90% assistido).
     *
     * @return true se completado
     */
    public boolean isEpisodeCompleted() {
        return getCompletionPercentage() >= 90.0;
    }

    /**
     * Tempo restante do episódio atual.
     *
     * @return duração restante
     */
    public Duration getRemainingDuration() {
        return totalDuration.minus(watchedDuration);
    }

    /**
     * Representação formatada do progresso (ex: "S02E05 - 15:30/45:00 (34%)").
     *
     * @return string formatada
     */
    public String format() {
        return String.format("S%02dE%02d - %s/%s (%.1f%%)",
                currentSeason,
                currentEpisode,
                formatDuration(watchedDuration),
                formatDuration(totalDuration),
                getCompletionPercentage());
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.toSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    // ==================== Getters ====================

    public int getCurrentSeason() {
        return currentSeason;
    }

    public int getCurrentEpisode() {
        return currentEpisode;
    }

    public Duration getWatchedDuration() {
        return watchedDuration;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    // ==================== Value Object Equality ====================

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SeriesProgress that))
            return false;
        return currentSeason == that.currentSeason
                && currentEpisode == that.currentEpisode
                && Objects.equals(watchedDuration, that.watchedDuration)
                && Objects.equals(totalDuration, that.totalDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentSeason, currentEpisode, watchedDuration, totalDuration);
    }

    @Override
    public String toString() {
        return format();
    }
}
