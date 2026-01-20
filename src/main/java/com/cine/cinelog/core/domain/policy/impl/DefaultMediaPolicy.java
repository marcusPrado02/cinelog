package com.cine.cinelog.core.domain.policy.impl;


import java.net.URI;
import java.net.URISyntaxException;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaPolicy;
import com.cine.cinelog.core.domain.vo.Title;
import com.cine.cinelog.core.domain.vo.Year;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

/**
 * Implementação padrão de {@link MediaPolicy}.
 *
 * Responsável por aplicar as invariantes de negócio principais
 * da entidade Media (M1–M5 no nosso catálogo de regras):
 *
 * - M1/M2: título obrigatório, normalizado e com tamanho máximo
 * - M3: tipo de mídia obrigatório
 * - M4: ano de lançamento em faixa válida (quando presente)
 * - M5: idioma original em formato ISO 639-1 (2 letras), ou null
 *
 * A validação é chamada a partir dos casos de uso (Create/UpdateMediaService).
 */
public class DefaultMediaPolicy implements MediaPolicy {

    private static final int DEFAULT_MIN_YEAR = 1888;
    private static final int DEFAULT_FUTURE_SLACK = 2;
    private static final int MAX_OVERVIEW_LENGTH = 4000;
    private static final int MAX_URL_LENGTH = 500;

    /**
     * Ano mínimo permitido para mídia (conceito de domínio).
     * Aqui usamos o mesmo valor de Year.MIN_YEAR para manter consistência.
     */
    private final int minYear;

    /**
     * Quantos anos à frente do ano atual ainda são aceitos.
     * Ex.: 1 permite cadastrar lançamentos do ano que vem.
     */
    private final int futureSlack;

    public DefaultMediaPolicy(int minYear, int futureSlack) {
        this.minYear = minYear;
        this.futureSlack = futureSlack;
    }

    @Measured("cinelog.policy.media.validate_invariants")
    @AlertIfSlow(thresholdMs = 100)
    @Override
    public void validateInvariants(Media media) {
        if (media == null) {
            // Falha de contrato: o caso de uso não deveria chamar com null
            throw DomainException.of(ErrorCode.GEN_VALIDATION);
        }

        // ===== M1 / M2: Título obrigatório + normalizado + tamanho máximo =====
        // Usa o VO Title para garantir as regras de atributo
        Title normalizedTitle = Title.of(media.getTitle());
        media.setTitle(normalizedTitle.value());

        // ===== M3: Tipo de mídia obrigatório =====
        if (media.getType() == null) {
            throw DomainException.of(ErrorCode.MEDIA_TYPE_REQUIRED);
        }

        // ===== M4: Ano de lançamento em faixa válida (quando presente) =====
        Integer year = media.getReleaseYear();
        if (year != null) {
            int current = java.time.Year.now().getValue();
            int maxYear = current + futureSlack;

            if (year < minYear || year > maxYear) {
                // usamos MEDIA_YEAR_OUT_OF_RANGE, pois é claramente uma regra do agregado Media
                throw DomainException.of(
                        ErrorCode.MEDIA_YEAR_OUT_OF_RANGE,
                        minYear,
                        maxYear,
                        year);
            }

            // opcional: se você quiser reaproveitar o VO Year, poderia ser:
            // Year.of(year);
        }

        // ===== M5: Idioma original como ISO 639-1 (2 letras) =====
        String lang = media.getOriginalLanguage();
        if (lang != null && !lang.isBlank()) {
            String normalizedLang = lang.trim().toLowerCase();
            boolean isAlpha2 = normalizedLang.length() == 2
                    && normalizedLang.chars().allMatch(Character::isLetter);

            if (!isAlpha2) {
                throw DomainException.of(ErrorCode.MEDIA_ORIGINAL_LANGUAGE_INVALID);
            }

            media.setOriginalLanguage(normalizedLang);
        } else {
            // normaliza vazio para null pra não ficar lixo no banco
            media.setOriginalLanguage(null);
        }

        // ===== M6: Overview com tamanho máximo =====
        String overview = media.getOverview();
        if (overview != null) {
            String trimmed = overview.trim();
            if (trimmed.length() > MAX_OVERVIEW_LENGTH) {
                // args: maxLength, actualLength
                throw DomainException.of(
                        ErrorCode.MEDIA_OVERVIEW_TOO_LONG,
                        MAX_OVERVIEW_LENGTH,
                        trimmed.length());
            }
            media.setOverview(trimmed.isEmpty() ? null : trimmed);
        }

        // ===== M7: URLs de poster/backdrop válidas =====
        validateImageUrl(media.getPosterUrl(), ErrorCode.MEDIA_POSTER_URL_INVALID,
                "posterUrl");
        validateImageUrl(media.getBackdropUrl(), ErrorCode.MEDIA_BACKDROP_URL_INVALID,
                "backdropUrl");
    }

    /**
     * Valida se a URL é vazia (ok), ou bem formada http/https.
     * Se inválida, dispara DomainException com ErrorCode específico.
     */
    private void validateImageUrl(String url, ErrorCode errorCode, String fieldName) {
        if (url == null || url.isBlank()) {
            return;
        }

        String trimmed = url.trim();
        if (trimmed.length() > MAX_URL_LENGTH) {
            throw DomainException.of(
                    errorCode,
                    MAX_URL_LENGTH,
                    trimmed.length());
        }

        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null ||
                    !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw DomainException.of(errorCode);
            }
        } catch (URISyntaxException e) {
            throw DomainException.of(errorCode);
        }
    }
}
