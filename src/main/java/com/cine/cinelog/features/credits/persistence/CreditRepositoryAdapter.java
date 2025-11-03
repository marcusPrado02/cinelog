package com.cine.cinelog.features.credits.persistence;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.mapper.CreditMapper;
import com.cine.cinelog.features.credits.repository.CreditJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
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
    public List<Credit> findAll() {
        return jpa.findAll().stream().map(creditMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}