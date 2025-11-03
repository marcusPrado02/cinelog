package com.cine.cinelog.core.application.usecase.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListMediaService implements ListMediaUseCase {

    private final MediaRepositoryPort repo;

    public ListMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<Media> execute(MediaType type, String q, int page, int size) {
        // Simples delegação para porta de persistência. A paginação e o
        // filtro são repassados conforme recebido pela camada de aplicação.
        return repo.find(type, q, page, size);
    }
}