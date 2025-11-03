package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;

public class MediaResponse {
    /**
     * DTO de resposta retornado pela API representando uma mídia.
     * Campos são públicos para simplicidade de serialização pelo Jackson.
     */
    public Long id;
    public String title;
    public MediaType type;
    public Integer releaseYear;
    public String originalTitle;
    public String originalLanguage;
    public String posterUrl;
    public String backdropUrl;
    public String overview;
}
