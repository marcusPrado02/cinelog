package com.cine.cinelog.core.application.usecase.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;

@Service
@Transactional(readOnly = true)
public class GetMediaService implements GetMediaUseCase {

    private final MediaRepositoryPort repo;

    public GetMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Media execute(Long id) {
        // Recupera a mídia pelo id ou lança exceção clara em português
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Mídia não encontrada: " + id));
    }
}