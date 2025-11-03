package com.cine.cinelog.core.application.usecase.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;

@Service
@Transactional
public class UpdateMediaService implements UpdateMediaUseCase {

    private final MediaRepositoryPort repo;

    public UpdateMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Media execute(Long id, Media data) {
        var current = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));
        var updated = current.updateFrom(data);
        return repo.save(updated);
    }
}
