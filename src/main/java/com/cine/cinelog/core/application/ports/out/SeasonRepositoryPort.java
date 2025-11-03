package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Season;
import java.util.List;
import java.util.Optional;

public interface SeasonRepositoryPort {
    Season save(Season season);

    Optional<Season> findById(Long id);

    List<Season> findAll();

    void deleteById(Long id);
}