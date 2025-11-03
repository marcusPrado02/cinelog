package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Credit;
import java.util.List;
import java.util.Optional;

public interface CreditRepositoryPort {
    Credit save(Credit credit);

    Optional<Credit> findById(Long id);

    List<Credit> findAll();

    void deleteById(Long id);
}
