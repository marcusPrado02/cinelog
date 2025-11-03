package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Episode;
import java.util.List;
import java.util.Optional;

public interface EpisodeRepositoryPort {
    Episode save(Episode episode);

    Optional<Episode> findById(Long id);

    List<Episode> findAll();

    void deleteById(Long id);
}