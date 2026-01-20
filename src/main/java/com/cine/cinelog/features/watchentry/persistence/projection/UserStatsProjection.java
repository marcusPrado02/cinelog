package com.cine.cinelog.features.watchentry.persistence.projection;

import java.time.LocalDate;
/**
 * Classe de configuração Spring para gerenciamento de userstatsprojection.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public interface UserStatsProjection {

    Long getUserId();

    Long getTotalEntries();

    Long getTotalRated();

    Double getAverageRating();

    LocalDate getFirstWatchDate();

    LocalDate getLastWatchDate();
}
