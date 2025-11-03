package com.cine.cinelog.core.application.ports.in.genre;

import com.cine.cinelog.core.domain.model.Genre;

public interface GetGenreUseCase {
    Genre execute(Long id);
}
