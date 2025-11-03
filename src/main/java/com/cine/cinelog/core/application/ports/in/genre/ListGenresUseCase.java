package com.cine.cinelog.core.application.ports.in.genre;

import com.cine.cinelog.core.domain.model.Genre;
import java.util.List;

public interface ListGenresUseCase {
    List<Genre> execute();
}