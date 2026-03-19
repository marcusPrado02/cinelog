package com.cine.cinelog.features.batch.jobs.reviews;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewResult;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.repository.UserJpaRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Grava reviews do TMDB como WatchEntries, criando usuários revisores quando
 * necessário.
 *
 * <p>
 * Cada review do TMDB é convertida em uma entrada de {@code watch_entry}
 * associada a um usuário fictício cujo email segue o padrão
 * {@code <username>@tmdb.cinelog.dev}. Se o usuário ainda não existir, ele é
 * criado automaticamente com a senha impossível de se logar.
 * </p>
 */
@Slf4j
@Component
public class TmdbReviewsItemWriter implements ItemWriter<ReviewsBundle> {

    private static final String EMAIL_DOMAIN = "@tmdb.cinelog.dev";
    /** Hash BCrypt de "tmdb-reviewer-no-login" — senha inutilizável para login. */
    private static final String LOCKED_PASSWORD_HASH = "$2a$10$XnMFJgXb6hkXQv9P2S9FGuGkWqxOvQdjVWB5Lw0g3S3/1DsPp6Hke";

    private final UserJpaRepository userJpaRepository;
    private final WatchEntryRepositoryPort watchEntryRepository;

    public TmdbReviewsItemWriter(UserJpaRepository userJpaRepository,
            WatchEntryRepositoryPort watchEntryRepository) {
        this.userJpaRepository = userJpaRepository;
        this.watchEntryRepository = watchEntryRepository;
    }

    @Override
    public void write(Chunk<? extends ReviewsBundle> chunk) {
        for (ReviewsBundle bundle : chunk) {
            for (TmdbReviewResult review : bundle.reviews()) {
                try {
                    processReview(bundle, review);
                } catch (Exception e) {
                    log.warn("Erro ao processar review id={} para mídia id={}: {}",
                            review.getId(), bundle.media().getId(), e.getMessage());
                }
            }
        }
    }

    private void processReview(ReviewsBundle bundle, TmdbReviewResult review) {
        String username = sanitizeUsername(review.getAuthorUsername());
        String email = username + EMAIL_DOMAIN;

        User reviewer = userJpaRepository.findByEmail(email)
                .map(this::entityToDomainUser)
                .orElseGet(() -> createReviewer(review.getAuthor(), username, email));

        if (reviewer.getId() == null) {
            log.warn("Falha ao criar/encontrar revisor para email={}", email);
            return;
        }

        LocalDate watchedAt = review.getCreatedAt() != null
                ? review.getCreatedAt().toLocalDate()
                : LocalDate.now();

        boolean alreadyExists = watchEntryRepository.existsByUserMediaEpisodeAndDate(
                reviewer.getId(), bundle.media().getId(), null, watchedAt);

        if (alreadyExists) {
            return;
        }

        BigDecimal ratingDecimal = null;
        if (review.getAuthorRating() != null) {
            long rounded = Math.round(review.getAuthorRating());
            if (rounded < 0)
                rounded = 0;
            if (rounded > 10)
                rounded = 10;
            ratingDecimal = BigDecimal.valueOf(rounded);
        }

        WatchEntry entry = new WatchEntry(
                null,
                reviewer.getId(),
                bundle.media().getId(),
                null,
                ratingDecimal,
                review.getContent(),
                watchedAt,
                null, null, null, null, null);
        entry.setStatusType(WatchEntryStatusType.COMPLETED);

        watchEntryRepository.save(entry);
        log.debug("Review salva: mídia={} usuário={}", bundle.media().getId(), reviewer.getEmail());
    }

    private User createReviewer(String displayName, String username, String email) {
        UserEntity entity = new UserEntity();
        entity.setName(displayName != null && !displayName.isBlank() ? displayName : username);
        entity.setEmail(email);
        entity.setPasswordHash(LOCKED_PASSWORD_HASH);
        entity.setRole("USER");
        entity.setEnabled(false); // reviewer accounts cannot log in
        UserEntity saved = userJpaRepository.save(entity);
        return entityToDomainUser(saved);
    }

    private User entityToDomainUser(UserEntity e) {
        User u = new User();
        u.setId(e.getId());
        u.setName(e.getName());
        u.setEmail(e.getEmail());
        return u;
    }

    private String sanitizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "reviewer-" + System.nanoTime();
        }
        return username.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
