package com.cine.cinelog.features.episodes.repository;

import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositório JPA para gerenciamento de episódios de séries.
 * 
 * <p>
 * Fornece operações de persistência para {@link EpisodeEntity}, incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca de episódios por temporada ordenados</li>
 * <li>Verificação de existência por mídia</li>
 * <li>Validação de unicidade (temporada + número do episódio)</li>
 * </ul>
 * 
 * @since 1.0
 * @see EpisodeEntity
 */
public interface EpisodeJpaRepository extends JpaRepository<EpisodeEntity, Long> {
    /**
     * Busca todos os episódios de uma temporada, ordenados por número.
     * 
     * @param seasonId o identificador da temporada
     * @return lista ordenada de episódios
     */
    List<EpisodeEntity> findBySeasonIdOrderByEpisodeNumberAsc(Long seasonId);

    /**
     * Verifica se existe algum episódio para a mídia especificada.
     * 
     * @param mediaId o identificador da mídia
     * @return true se houver episódios, false caso contrário
     */
    @Query("""
                SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
                FROM EpisodeEntity e
                JOIN SeasonEntity s ON e.seasonId = s.id
                WHERE s.mediaId = :mediaId
            """)
    boolean existsByMediaId(Long mediaId);

    /**
     * Verifica se um episódio específico existe para uma temporada.
     * 
     * @param seasonId      o identificador da temporada
     * @param episodeNumber o número do episódio
     * @return true se o episódio existir, false caso contrário
     */
    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EpisodeEntity e
            WHERE e.seasonId = :seasonId AND e.episodeNumber = :episodeNumber
            """)
    boolean existsBySeasonIdAndEpisodeNumber(Long seasonId, Integer episodeNumber);

    /**
     * Verifica se um episódio existe para uma temporada específica.
     * 
     * @param seasonId
     * @return
     */
    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM EpisodeEntity e
            WHERE e.seasonId = :seasonId
            """)
    boolean existsBySeasonId(Long seasonId);
}
