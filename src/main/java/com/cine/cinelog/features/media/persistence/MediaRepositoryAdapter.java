package com.cine.cinelog.features.media.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.repository.MediaJpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class MediaRepositoryAdapter implements MediaRepositoryPort {

    /**
     * Adapter que implementa a porta de persistência `MediaRepositoryPort`
     * usando Spring Data JPA (`MediaJpaRepository`).
     *
     * Esta classe faz o mapeamento entre o modelo de domínio `Media` e a
     * entidade JPA `MediaEntity` através de `MediaEntityMapper` e delega as
     * operações ao repositório JPA.
     */

    private final MediaJpaRepository repository;
    private final MediaMapper mapper;

    public MediaRepositoryAdapter(MediaJpaRepository repository, MediaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Media save(Media media) {
        MediaEntity saved = repository.save(mapper.toEntity(media));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Media> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<Media> find(MediaType type, String q, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        var result = (type != null && q != null && !q.isBlank())
                ? repository.findByTypeAndTitleContainingIgnoreCase(type, q, pageable)
                : (type != null)
                        ? repository.findByType(type, pageable)
                        : (q != null && !q.isBlank())
                                ? repository.findByTitleContainingIgnoreCase(q, pageable)
                                : repository.findAll(pageable);
        return result.map(mapper::toDomain).getContent();
    }
}
