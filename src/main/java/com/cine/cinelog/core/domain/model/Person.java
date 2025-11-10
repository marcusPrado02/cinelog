package com.cine.cinelog.core.domain.model;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Person {
    private Long id;
    private String name;
    private LocalDate birthDate;
    private String placeOfBirth;

}