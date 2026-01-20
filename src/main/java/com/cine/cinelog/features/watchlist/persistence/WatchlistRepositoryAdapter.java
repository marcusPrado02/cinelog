package com.cine.cinelog.features.watchlist.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.features.watchlist.mapper.WatchlistMapper;
import com.cine.cinelog.features.watchlist.repository.WatchlistJpaRepository;

/**
 * Adaptador de repositório para persistência de itens da Watchlist.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 *
 * @since 1.0
 * @see WatchlistRepositoryPort
 * @see WatchlistItemEntity
 * @see WatchlistItem
 */
@Repository
public class WatchlistRepositoryAdapter implements WatchlistRepositoryPort {

    private final WatchlistJpaRepository jpaRepository;
    private final WatchlistMapper mapper;

    public WatchlistRepositoryAdapter(WatchlistJpaRepository jpaRepository, WatchlistMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public WatchlistItem save(WatchlistItem item) {
        WatchlistItemEntity entity = mapper.toEntity(item);
        WatchlistItemEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WatchlistItem> findByUserIdAndMediaId(Long userId, Long mediaId) {
        return jpaRepository.findByUserIdAndMediaId(userId, mediaId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<WatchlistItem> findAllByUserId(Long userId) {
        List<WatchlistItemEntity> items = jpaRepository.findAllByUserIdOrderByAddedAtDesc(userId);
        List<WatchlistItem> domainItems = items.stream()
                .map(mapper::toDomain)
                .toList();

        // Retorna um PageResult simples sem paginação real (lista completa)
        int totalElements = domainItems.size();
        int totalPages = totalElements > 0 ? 1 : 0;
        return new PageResult<>(domainItems, 0, totalElements, totalElements, totalPages);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByMediaId(Long mediaId) {
        return jpaRepository.existsByMediaId(mediaId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
