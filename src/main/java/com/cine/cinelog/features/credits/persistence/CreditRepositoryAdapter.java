package com.cine.cinelog.features.credits.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import com.cine.cinelog.features.credits.repository.CreditJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de repositório para persistência de Credit.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 *
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre Credit e CreditEntity.
 * </p>
 *
 * @since 1.0
 * @see CreditRepositoryPort
 * @see CreditEntity
 * @see Credit
 */
@Repository
public class CreditRepositoryAdapter implements CreditRepositoryPort {

    private final CreditJpaRepository jpa;
    private final CreditMapper creditMapper;

    public CreditRepositoryAdapter(CreditJpaRepository jpa, CreditMapper creditMapper) {
        this.jpa = jpa;
        this.creditMapper = creditMapper;
    }

    @Override
    public Credit save(Credit credit) {
        var e = creditMapper.toEntity(credit);
        var s = jpa.save(e);
        return creditMapper.toDomain(s);
    }

    @Override
    public Optional<Credit> findById(Long id) {
        return jpa.findById(id).map(creditMapper::toDomain);
    }

    @Override
    public PageResult<Credit> findAll(PageQuery query) {

        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort()));

        Page<CreditEntity> page = jpa.findAll(pageable);

        return PageResultMapper.from(page, creditMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByMediaIdAndPersonIdAndRole(Long mediaId, Long personId, String role) {
        return jpa.existsByMediaIdAndPersonIdAndRole(mediaId, personId, role);
    }
}
