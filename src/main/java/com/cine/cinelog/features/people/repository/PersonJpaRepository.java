package com.cine.cinelog.features.people.repository;

import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonJpaRepository extends JpaRepository<PersonEntity, Long> {
    Page<PersonEntity> findByNameContainingIgnoreCase(String name, Pageable pg);
}