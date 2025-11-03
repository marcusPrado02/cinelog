package com.cine.cinelog.core.application.usecase.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;

@Service
@Transactional
public class DeleteMediaService implements DeleteMediaUseCase {

    private final MediaRepositoryPort repo;

    public DeleteMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        // Remove a mídia com o id informado. A implementação da porta
        // deve decidir se lança erro quando o id não existe.
        repo.deleteById(id);
    }
}