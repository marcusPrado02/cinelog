package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Season {
    private Long id;
    private Long mediaId;
    private Integer seasonNumber;
    private String name;
    private LocalDate airDate;

}
