package com.cine.cinelog.core.application.ports.in.season;

import com.cine.cinelog.core.domain.model.Season;

public interface GetSeasonUseCase {
    Season execute(Long id);
}