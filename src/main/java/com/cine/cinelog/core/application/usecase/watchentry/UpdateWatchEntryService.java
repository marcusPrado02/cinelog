package com.cine.cinelog.core.application.usecase.watchentry;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.watchentry.UpdateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;

@Service
@Transactional
public class UpdateWatchEntryService implements UpdateWatchEntryUseCase {

    private final WatchEntryRepositoryPort repo;
    private final WatchEntryPolicy watchPolicy;
    private final RatingPolicy ratingPolicy;

    public UpdateWatchEntryService(WatchEntryRepositoryPort repo,
            WatchEntryPolicy watchPolicy,
            RatingPolicy ratingPolicy) {
        this.repo = repo;
        this.watchPolicy = watchPolicy;
        this.ratingPolicy = ratingPolicy;
    }

    @Override
    public WatchEntry execute(WatchEntry entry, boolean isRatingOperation) {
        watchPolicy.validateUpdate(entry);

        if (isRatingOperation && entry.getRating() != null) {
            ratingPolicy.validateCanRate(entry, entry.getRating(), Instant.now());
        }

        entry.applyRating(entry.getRating(), entry.getComment());
        return repo.save(entry);
    }
}
