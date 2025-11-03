package com.cine.cinelog.core.application.ports.in.media;

import com.cine.cinelog.core.domain.model.Media;

public interface UpdateMediaUseCase {
    Media execute(Long id, Media data);
}
