package com.cine.cinelog.features.batch.jobs.media;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Tasklet que vincula registros de midia legados (seed) ao TMDB.
 *
 * <p>Para cada midia sem {@code tmdbId}, busca pelo titulo na TMDB Search API,
 * vincula o {@code tmdbId} e preenche {@code posterUrl} e {@code backdropUrl}
 * a partir dos detalhes completos do TMDB.</p>
 *
 * <p>Fluxo por midia:
 * <ol>
 *   <li>Search API: {@code /search/movie?query=<title>&year=<year>}</li>
 *   <li>Seleciona o primeiro resultado (maior relevancia)</li>
 *   <li>Details API: {@code /movie/<tmdbId>} para obter poster/backdrop completos</li>
 *   <li>Persiste tmdbId + imagens no banco</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkTmdbTasklet implements Tasklet {

    private final MediaRepositoryPort mediaRepository;
    private final TmdbClientPort tmdbClient;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Media> unlinked = mediaRepository.findAllWithoutTmdbId();
        log.info("[BATCH] LinkTmdbTasklet: {} midias sem tmdbId encontradas", unlinked.size());

        int linked = 0;
        int enriched = 0;
        int notFound = 0;

        for (Media media : unlinked) {
            try {
                Long tmdbId = searchTmdbId(media);
                if (tmdbId == null) {
                    notFound++;
                    log.debug("TMDB nao encontrou match para '{}' ({})", media.getTitle(), media.getType());
                    continue;
                }

                // Verificar se outra midia ja tem esse tmdbId (evitar duplicata)
                if (mediaRepository.findByTmdbId(tmdbId).isPresent()) {
                    log.debug("tmdbId={} ja existe no banco, pulando '{}'", tmdbId, media.getTitle());
                    continue;
                }

                media.setTmdbId(tmdbId);
                linked++;

                // Buscar detalhes completos para imagens
                Optional<TmdbMediaDetails> details = fetchDetails(media.getType(), tmdbId);
                if (details.isPresent()) {
                    TmdbMediaDetails d = details.get();
                    if (d.getPosterUrl() != null) {
                        media.setPosterUrl(d.getPosterUrl());
                    }
                    if (d.getBackdropUrl() != null) {
                        media.setBackdropUrl(d.getBackdropUrl());
                    }
                    if (d.getOverview() != null && (media.getOverview() == null || media.getOverview().isBlank())) {
                        media.setOverview(d.getOverview());
                    }
                    if (d.getOriginalTitle() != null) {
                        media.setOriginalTitle(d.getOriginalTitle());
                    }
                    if (d.getOriginalLanguage() != null) {
                        media.setOriginalLanguage(d.getOriginalLanguage());
                    }
                    enriched++;
                }

                mediaRepository.save(media);

            } catch (Exception e) {
                log.warn("Erro ao vincular TMDB para midia id={} '{}': {}",
                        media.getId(), media.getTitle(), e.getMessage());
            }
        }

        log.info("[BATCH] LinkTmdbTasklet concluido: linked={} enriched={} notFound={} total={}",
                linked, enriched, notFound, unlinked.size());

        contribution.incrementWriteCount(linked);

        return RepeatStatus.FINISHED;
    }

    private Long searchTmdbId(Media media) {
        TmdbSearchResult<TmdbMediaSummary> result;

        if (media.getType() == MediaType.MOVIE) {
            result = tmdbClient.searchMovies(media.getTitle(), media.getReleaseYear(), 1);
        } else {
            result = tmdbClient.searchTvShows(media.getTitle(), media.getReleaseYear(), 1);
        }

        if (result == null || result.getResults() == null || result.getResults().isEmpty()) {
            return null;
        }

        return result.getResults().getFirst().getTmdbId();
    }

    private Optional<TmdbMediaDetails> fetchDetails(MediaType type, Long tmdbId) {
        if (type == MediaType.MOVIE) {
            return tmdbClient.fetchMovie(tmdbId);
        } else {
            return tmdbClient.fetchTv(tmdbId);
        }
    }
}
