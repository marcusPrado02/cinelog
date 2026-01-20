package com.cine.cinelog.core.application.usecase.media;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.SyncMediaFromTmdbUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.application.ports.out.MediaGenreLinkPort;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável por sincronizar dados de mídias com a API do TMDb (The
 * Movie Database).
 *
 * <p>
 * Estratégia:
 * <ol>
 * <li>Tenta buscar pelo tmdbId, se a mídia já tiver;</li>
 * <li>Se não encontrar, busca por título + ano;</li>
 * <li>Se encontrar, atualiza campos principais e gêneros (tabela
 * media_genres);</li>
 * <li>Se nada for encontrado, retorna a mídia como está.</li>
 * </ol>
 * </p>
 */
@Transactional
public class SyncMediaFromTmdbService implements SyncMediaFromTmdbUseCase {
    private static final Logger log = LoggerFactory.getLogger(SyncMediaFromTmdbService.class);

    private final MediaRepositoryPort mediaRepository;
    private final TmdbClientPort tmdbClient;
    private final GenreRepositoryPort genreRepository;
    private final MediaGenreLinkPort mediaGenreLinkPort;

    public SyncMediaFromTmdbService(MediaRepositoryPort mediaRepository,
            TmdbClientPort tmdbClient,
            GenreRepositoryPort genreRepository,
            MediaGenreLinkPort mediaGenreLinkPort) {
        this.mediaRepository = mediaRepository;
        this.tmdbClient = tmdbClient;
        this.genreRepository = genreRepository;
        this.mediaGenreLinkPort = mediaGenreLinkPort;
    }

    /**
     * Sincroniza os dados de uma mídia com informações atualizadas do TMDb.
     *
     * @param mediaId o identificador local da mídia a ser sincronizada
     * @return a mídia atualizada com dados do TMDb (ou a mídia original, se não
     *         houver dados no TMDb)
     */
    @Override
    @Observed(name = "media.sync-tmdb", contextualName = "sync-media-from-tmdb-service")
    @Measured("cinelog.service.media.sync-tmdb")
    @AlertIfSlow(thresholdMs = 2000, metricName = "cinelog.slow_sync_tmdb")
    @AuditableAction(module = "MEDIA", action = "SYNC_FROM_TMDB", description = "Sincronização de dados com TMDb")
    public Media sync(Long mediaId) {
        log.debug("Iniciando sincronização com TMDb. MediaId: {}", mediaId);

        try {
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.MEDIA_NOT_FOUND, "Media not found: " + mediaId));

            log.debug("Mídia encontrada. Título: {}, TmdbId: {}",
                    media.getTitle(), media.getTmdbId());

            Optional<TmdbMediaDetails> tmdbResult = Optional.empty();

            // 1) Preferencial: buscar pelo tmdbId se já existir
            if (media.hasTmdbId()) {
                log.debug("Buscando no TMDb por ID. TmdbId: {}", media.getTmdbId());
                tmdbResult = tmdbClient.fetchByTmdbId(media.getTmdbId());
            }

            // 2) Fallback: buscar por título + ano
            if (tmdbResult.isEmpty()) {
                log.debug("Buscando no TMDb por título e ano. Título: {}, Ano: {}",
                        media.getTitle(), media.getReleaseYear());
                tmdbResult = tmdbClient.searchByTitleAndYear(media.getTitle(), media.getReleaseYear());
            }

            // 3) Se encontrou alguma coisa no TMDb, aplica os detalhes
            if (tmdbResult.isPresent()) {
                TmdbMediaDetails tmdbDetails = tmdbResult.get();
                log.debug("Dados encontrados no TMDb. Aplicando atualizações");

                applyTmdbDetails(media, tmdbDetails); // Atualiza campos da Media
                mediaRepository.save(media); // Persiste a mídia

                syncGenres(media, tmdbDetails); // Atualiza tabela media_genres

                log.info("Sincronização com TMDb concluída com sucesso. MediaId: {}, TmdbId: {}, Título: {}",
                        mediaId, tmdbDetails.getTmdbId(), tmdbDetails.getTitle());
            } else {
                log.warn("Nenhum dado encontrado no TMDb. MediaId: {}, Título: {}",
                        mediaId, media.getTitle());
            }

            return media;
        } catch (DomainException e) {
            log.warn("Erro de domínio ao sincronizar com TMDb. MediaId: {}, Erro: {}",
                    mediaId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao sincronizar com TMDb. MediaId: {}, Erro: {}",
                    mediaId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Aplica os campos de TmdbMediaDetails na entidade Media.
     */
    private void applyTmdbDetails(Media media, TmdbMediaDetails tmdbDetails) {
        media.setTmdbId(tmdbDetails.getTmdbId());
        media.setTitle(tmdbDetails.getTitle());
        media.setOriginalTitle(tmdbDetails.getOriginalTitle());
        media.setOverview(tmdbDetails.getOverview());
        media.setReleaseYear(tmdbDetails.getReleaseYear());
        media.setPosterUrl(tmdbDetails.getPosterUrl());
        media.setBackdropUrl(tmdbDetails.getBackdropUrl());
        media.setOriginalLanguage(tmdbDetails.getOriginalLanguage());

        // Se fizer sentido no seu modelo:
        if (tmdbDetails.getType() != null) {
            media.setType(tmdbDetails.getType());
        }
    }

    /**
     * Sincroniza os gêneros da mídia com base na lista de nomes vinda do TMDb.
     *
     * Estratégia:
     * - Garante que cada nome de gênero exista em Genre (criando se necessário);
     * - Atualiza a tabela media_genres para refletir exatamente a lista do TMDb.
     */
    private void syncGenres(Media media, TmdbMediaDetails tmdbDetails) {
        var genreNames = tmdbDetails.getGenres();

        if (genreNames == null || genreNames.isEmpty()) {
            // Estratégia simples: se o TMDb não mandar gêneros, não mexe nos links atuais
            return;
        }

        // Garante existência dos gêneros e coleta IDs
        List<Long> genreIds = genreNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .map(this::findOrCreateGenre)
                .map(Genre::getId)
                .toList();

        // Atualiza a tabela media_genres via porta
        mediaGenreLinkPort.replaceGenres(media.getId(), genreIds);
    }

    private Genre findOrCreateGenre(String name) {
        return genreRepository.findByName(name)
                .orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setName(name);
                    return genreRepository.save(genre);
                });
    }
}
