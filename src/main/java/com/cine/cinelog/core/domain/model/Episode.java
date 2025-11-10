package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Episode {
    private Long id;
    private Long seasonId;
    private Integer episodeNumber;
    private String name;
    private LocalDate airDate;

}
