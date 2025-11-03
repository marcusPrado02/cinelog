package com.cine.cinelog.features.credits.repository;

import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditJpaRepository extends JpaRepository<CreditEntity, Long> {
    List<CreditEntity> findByMediaIdOrderByOrderIndexAsc(Long mediaId);
}
