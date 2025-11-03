package com.cine.cinelog.features.people.web.dto;

import java.time.LocalDate;

public record PersonResponse(Long id, String name, LocalDate birthDate, String placeOfBirth) {
}