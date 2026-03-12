package com.cine.cinelog.features.batch.jobs.genres;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.model.tmdb.TmdbGenre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Tasklet que sincroniza os gêneros de filmes e séries do TMDB
 * com o banco de dados local. É idempotente: não duplica gêneros
 * existentes e apenas insere os novos.
 */
@Component
public class SyncGenresTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SyncGenresTasklet.class);

    private final TmdbClientPort tmdbClient;
    private final GenreRepositoryPort genreRepository;

    public SyncGenresTasklet(TmdbClientPort tmdbClient, GenreRepositoryPort genreRepository) {
        this.tmdbClient = tmdbClient;
        this.genreRepository = genreRepository;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        log.info("Iniciando sincronização de gêneros do TMDB...");

        List<TmdbGenre> movieGenres = tmdbClient.fetchMovieGenres();
        List<TmdbGenre> tvGenres = tmdbClient.fetchTvGenres();

        List<TmdbGenre> all = new ArrayList<>(movieGenres);
        tvGenres.stream()
                .filter(tv -> all.stream().noneMatch(m -> m.getName().equalsIgnoreCase(tv.getName())))
                .forEach(all::add);

        int created = 0;
        int skipped = 0;

        for (TmdbGenre tmdbGenre : all) {
            if (tmdbGenre.getName() == null || tmdbGenre.getName().isBlank()) {
                continue;
            }
            boolean exists = genreRepository.findByName(tmdbGenre.getName()).isPresent();
            if (exists) {
                skipped++;
            } else {
                Genre genre = new Genre();
                genre.setName(tmdbGenre.getName());
                genreRepository.save(genre);
                created++;
            }
        }

        log.info("Sincronização de gêneros concluída: {} criados, {} já existentes.", created, skipped);
        contribution.incrementWriteCount(created);

        return RepeatStatus.FINISHED;
    }
}
