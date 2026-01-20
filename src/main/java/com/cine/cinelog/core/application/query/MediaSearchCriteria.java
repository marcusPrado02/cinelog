package com.cine.cinelog.core.application.query;

import java.util.List;

import com.cine.cinelog.core.domain.enums.MediaType;

import lombok.Getter;
import lombok.Setter;

@Getter
/**
 * Classe de configuração Spring para gerenciamento de mediasearchcriteria.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */
@Setter
public class MediaSearchCriteria {
        private int page;
        private int size;
        private String text; // busca geral (título, descrição, pessoas)
        private MediaType type;
        private Integer yearMin;
        private Integer yearMax;
        private Double ratingMin;
        private Double ratingMax;
        private List<Long> genreIds;
        private SortQuery sort;
}
