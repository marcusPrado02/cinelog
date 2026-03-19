package com.cine.cinelog.features.reports.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;

/**
 * Request body for sending a report to a specific email address.
 *
 * @param email the recipient email; if null the authenticated user's email is used
 * @param limit optional item limit (used by top-rated report; default 10)
 */
public record SendReportRequest(
        @Email(message = "email deve ser um endereço válido")
        @Schema(description = "Recipient email (optional — defaults to authenticated user's email)") String email,

        @Positive(message = "limit deve ser > 0")
        @Schema(description = "Max number of items in the report (default 10)", example = "10") Integer limit) {

    public int effectiveLimit() {
        return (limit != null && limit > 0) ? limit : 10;
    }
}
