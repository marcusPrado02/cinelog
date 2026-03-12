package com.cine.cinelog.features.media.repository;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import jakarta.persistence.criteria.JoinType;

/**
 * Classe de configuração Spring para gerenciamento de mediaspecifications.
 *
 * <p>
 * Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.
 * </p>
 *
 * @since 1.0
 */

public final class MediaSpecifications {

    private MediaSpecifications() {
    }

    public static Specification<MediaEntity> byCriteria(MediaSearchCriteria c) {
        return Specification
                .where(textContains(c.getText()))
                .and(hasType(c.getType()))
                .and(yearGreaterOrEqual(c.getYearMin()))
                .and(yearLessOrEqual(c.getYearMax()))
                .and(ratingGreaterOrEqual(c.getRatingMin()))
                .and(ratingLessOrEqual(c.getRatingMax()))
                .and(hasGenres(c.getGenreIds()));
    }

    private static Specification<MediaEntity> textContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            // MySQL LIKE é case-insensitive por padrão; evitar cb.lower() no campo
            // overview pois é @Lob/CLOB — lower() rejeita CLOB no Hibernate/MySQL.
            String like = "%" + text + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("overview"), like));
        };
    }

    private static Specification<MediaEntity> hasType(MediaType type) {
        return (root, query, cb) -> {
            if (type == null)
                return null;
            return cb.equal(root.get("type"), type);
        };
    }

    private static Specification<MediaEntity> yearGreaterOrEqual(Integer yearMin) {
        return (root, query, cb) -> {
            if (yearMin == null)
                return null;
            return cb.greaterThanOrEqualTo(root.get("releaseYear"), yearMin);
        };
    }

    private static Specification<MediaEntity> yearLessOrEqual(Integer yearMax) {
        return (root, query, cb) -> {
            if (yearMax == null)
                return null;
            return cb.lessThanOrEqualTo(root.get("releaseYear"), yearMax);
        };
    }

    private static Specification<MediaEntity> ratingGreaterOrEqual(Double ratingMin) {
        return (root, query, cb) -> {
            if (ratingMin == null)
                return null;
            return cb.greaterThanOrEqualTo(root.get("averageRating"), ratingMin);
        };
    }

    private static Specification<MediaEntity> ratingLessOrEqual(Double ratingMax) {
        return (root, query, cb) -> {
            if (ratingMax == null)
                return null;
            return cb.lessThanOrEqualTo(root.get("averageRating"), ratingMax);
        };
    }

    private static Specification<MediaEntity> hasGenres(List<Long> genreIds) {
        return (root, query, cb) -> {
            if (genreIds == null || genreIds.isEmpty())
                return null;

            var join = root.join("genres", JoinType.INNER);
            query.distinct(true);

            return join.get("id").in(genreIds);
        };
    }
}
