package com.cine.cinelog.features.media.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.model.MediaWithRating;
import com.cine.cinelog.features.media.mapper.MediaMapper;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.projection.MediaWithRatingProjection;
import com.cine.cinelog.features.media.repository.MediaJpaRepository;
import com.cine.cinelog.features.media.repository.MediaSpecifications;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import java.util.List;
import java.util.Optional;

/**
 * Adapter que implementa a porta de persistência `MediaRepositoryPort`
 * usando Spring Data JPA (`MediaJpaRepository`).
 *
 * Esta classe faz o mapeamento entre o modelo de domínio `Media` e a
 * entidade JPA `MediaEntity` através de `MediaEntityMapper` e delega as
 * operações ao repositório JPA.
 */
/**
 * Adaptador de repositório para persistência de Media.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 *
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre Media e MediaEntity.
 * </p>
 *
 * @since 1.0
 * @see MediaRepositoryPort
 * @see MediaEntity
 * @see Media
 */
@Repository
public class MediaRepositoryAdapter implements MediaRepositoryPort {

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
    @Measured("cinelog.repository.search_media")
    @AlertIfSlow(thresholdMs = 500, metricName = "cinelog.slow_db_search")
    public PageResult<Media> search(MediaSearchCriteria criteria, PageQuery pageQuery) {
        var pageable = PageRequest.of(pageQuery.page(), pageQuery.size());
        var spec = MediaSpecifications.byCriteria(criteria);

        Page<MediaEntity> page = repository.findAll(spec, (Pageable) pageable);

        return PageResultMapper.from(page, mapper::toDomain);
    }

    @Override
    public PageResult<Media> listAll(PageQuery query) {
        PageRequest pageRequest = PageRequest.of(query.page(), query.size());
        var result = repository.findAll(pageRequest);
        return PageResultMapper.from(result, mapper::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    @Measured("cinelog.repository.find_candidates")
    @AlertIfSlow(thresholdMs = 800, metricName = "cinelog.slow_recommendation_query")
    public List<MediaWithRating> findCandidatesForUser(Long userId) {
        List<MediaWithRatingProjection> projections = repository.findCandidatesForUser(userId);

        return projections.stream()
                .map(item -> new MediaWithRating(
                        item.getMediaId(),
                        item.getTitle(),
                        MediaType.valueOf(item.getType()),
                        item.getAverageRating(),
                        item.getRatingCount()))
                .toList();
    }

    @Override
    public Optional<Media> findByTmdbId(Long tmdbId) {
        return repository.findByTmdbId(tmdbId).map(mapper::toDomain);
    }

    @Override
    public List<Media> findAllWithTmdbId(MediaType type) {
        return repository.findByTypeAndTmdbIdNotNull(type)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Media> findAllMissingImages() {
        return repository.findAllMissingImages()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
