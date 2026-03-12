package com.cine.cinelog.features.credits.repository;

import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório JPA para gerenciamento de créditos (participações de pessoas em
 * mídias).
 *
 * <p>
 * Fornece operações de persistência para {@link CreditEntity}, incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca de créditos por mídia ordenados</li>
 * </ul>
 *
 * @since 1.0
 * @see CreditEntity
 */
public interface CreditJpaRepository extends JpaRepository<CreditEntity, Long> {
    /**
     * Busca todos os créditos de uma mídia, ordenados pelo índice de exibição.
     *
     * @param mediaId o identificador da mídia
     * @return lista ordenada de créditos
     */
    List<CreditEntity> findByMediaIdOrderByOrderIndexAsc(Long mediaId);

    /**
     * Verifica se existe um crédito para uma combinação específica de mídia, pessoa
     * e papel.
     *
     * @param mediaId  ID da mídia
     * @param personId ID da pessoa
     * @param role     papel
     * @return true se existir
     */
    boolean existsByMediaIdAndPersonIdAndRole(Long mediaId, Long personId, String role);
}
