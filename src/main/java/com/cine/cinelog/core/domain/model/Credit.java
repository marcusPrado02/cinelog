package com.cine.cinelog.core.domain.model;

import com.cine.cinelog.core.domain.enums.Role;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Credit {
    private Long id;
    private Long mediaId;
    private Long personId;
    private Role role;
    private String characterName;
    private Short orderIndex;

}