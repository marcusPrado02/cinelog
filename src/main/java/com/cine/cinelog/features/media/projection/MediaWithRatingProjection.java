package com.cine.cinelog.features.media.projection;

import java.math.BigDecimal;
/**
 * Classe de configuração Spring para gerenciamento de mediawithratingprojection.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */

public interface MediaWithRatingProjection {
    Long getMediaId();

    String getTitle();

    String getType();

    BigDecimal getAverageRating();

    long getRatingCount();

}
