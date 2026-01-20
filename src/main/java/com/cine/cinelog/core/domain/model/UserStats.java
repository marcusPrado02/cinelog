package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import java.util.Objects;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
/**
 * Representa UserStats no domínio do sistema.
 * 
 * <p>Esta classe encapsula os conceitos e regras de negócio relacionados a userstats.
 * Contém a lógica de domínio pura, independente de frameworks e infraestrutura.</p>
 * 
 * @since 1.0
 */
@NoArgsConstructor
public class UserStats extends Auditable {

    private Long userId;
    private long totalEntries; // quantos itens assistidos
    private long totalRated; // quantos com nota
    private Double averageRating; // média das notas (pode ser null)
    private LocalDate firstWatchDate;
    private LocalDate lastWatchDate;

    /**
     * Fábrica que aplica ST1 e ST2.
     */
    public static UserStats of(Long userId,
            long totalEntries,
            long totalRated,
            Double rawAverageRating,
            LocalDate firstWatchDate,
            LocalDate lastWatchDate) {

        if (totalEntries < 0 || totalRated < 0) {
            throw DomainException.of(
                    ErrorCode.INVALID_ARGUMENT,
                    "Stats cannot have negative counts");
        }

        // ST1: totalRated <= totalEntries
        if (totalRated > totalEntries) {
            throw DomainException.of(
                    ErrorCode.INVALID_ARGUMENT,
                    "totalRated cannot be greater than totalEntries");
        }

        // ST2: averageRating deve ser null se totalRated == 0
        Double normalizedAverage;
        if (totalRated == 0) {
            normalizedAverage = null;
        } else {
            if (rawAverageRating == null) {
                throw DomainException.of(
                        ErrorCode.INVALID_ARGUMENT,
                        "averageRating cannot be null when totalRated > 0");
            }
            normalizedAverage = rawAverageRating;
        }

        return new UserStats(userId, totalEntries, totalRated, normalizedAverage, firstWatchDate, lastWatchDate);
    }

    @Override
    public String toString() {
        return "UserStats{" +
                "userId=" + userId +
                ", totalEntries=" + totalEntries +
                ", totalRated=" + totalRated +
                ", averageRating=" + averageRating +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserStats))
            return false;
        UserStats that = (UserStats) o;
        return totalEntries == that.totalEntries
                && totalRated == that.totalRated
                && Objects.equals(userId, that.userId)
                && Objects.equals(averageRating, that.averageRating);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, totalEntries, totalRated, averageRating);
    }

}
