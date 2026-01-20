package com.cine.cinelog.features.seasons.repository;

import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositório JPA para gerenciamento de temporadas de séries.
 * 
 * <p>
 * Fornece operações de persistência para {@link SeasonEntity}, incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca de temporadas por mídia ordenadas</li>
 * <li>Verificação de existência</li>
 * <li>Validação de unicidade (mídia + número da temporada)</li>
 * </ul>
 * 
 * @since 1.0
 * @see SeasonEntity
 */
public interface SeasonJpaRepository extends JpaRepository<SeasonEntity, Long> {
        /**
         * Busca todas as temporadas de uma mídia, ordenadas por número.
         * 
         * @param mediaId o identificador da mídia (série)
         * @return lista ordenada de temporadas
         */
        List<SeasonEntity> findByMediaIdOrderBySeasonNumberAsc(Long mediaId);

        /**
         * Verifica se existe alguma temporada para a mídia especificada.
         * 
         * @param mediaId o identificador da mídia
         * @return true se houver temporadas, false caso contrário
         */
        boolean existsByMediaId(Long mediaId);

        /**
         * Verifica se uma temporada específica existe para uma mídia.
         * 
         * @param mediaId      o identificador da mídia
         * @param seasonNumber o número da temporada
         * @return true se a temporada existir, false caso contrário
         */
        @Query("""
                        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
                        FROM SeasonEntity s
                        WHERE s.mediaId = :mediaId AND s.seasonNumber = :seasonNumber
                        """)
        boolean existsByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber);

        /**
         * Verifica se uma temporada existe pelo seu ID.
         * 
         * @param id
         * @return
         */
        @Query("""
                        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
                        FROM SeasonEntity s
                        WHERE s.id = :id
                        """)
        boolean existsById(Long id);
}