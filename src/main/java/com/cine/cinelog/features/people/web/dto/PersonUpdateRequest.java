package com.cine.cinelog.features.people.web.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PersonUpdateRequest(
                @NotBlank @Size(max = 200) String name,
                LocalDate birthDate,
                @Size(max = 200) String placeOfBirth) {
}