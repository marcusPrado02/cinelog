package com.cine.cinelog.core.application.events.mappers;

import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Mapper seguro para WatchEntryCreatedEvent.
 * <p>
 * Garante que apenas campos allowlisted são incluídos no evento.
 * Nenhum dado sensível (email, password, token) é exposto.
 * <p>
 * PR5: Event Security - PII Protection
 */
@Component
public class WatchEntryCreatedPayloadMapper implements EventPayloadMapper<WatchEntry, WatchEntryCreatedEvent> {

    /**
     * Campos permitidos (allowlist) para WatchEntryCreatedEvent.
     * <p>
     * APENAS estes campos podem ser incluídos no evento.
     * Qualquer campo adicional deve ser explicitamente adicionado aqui após revisão
     * de segurança.
     */
    private static final String[] ALLOWED_FIELDS = {
            "watchEntryId",
            "userId",
            "mediaId",
            "episodeId",
            "watchedAt",
            "rating"
    };

    /**
     * Campos proibidos que NUNCA devem aparecer em eventos.
     * <p>
     * Validação automática garante que estes não vazem.
     */
    private static final List<String> FORBIDDEN_FIELDS = Arrays.asList(
            "email",
            "password",
            "token",
            "secret",
            "apiKey",
            "privateKey",
            "ssn",
            "creditCard");

    @Override
    public WatchEntryCreatedEvent toEventPayload(WatchEntry watchEntry) {
        if (watchEntry == null) {
            throw new IllegalArgumentException("WatchEntry cannot be null");
        }

        // Mapeia apenas campos allowlisted
        return WatchEntryCreatedEvent.of(
                watchEntry.getId(),
                watchEntry.getUserId(),
                watchEntry.getMediaId(),
                watchEntry.getEpisodeId(),
                watchEntry.getWatchedAt(),
                watchEntry.getRating());
    }

    @Override
    public void validatePayload(WatchEntryCreatedEvent payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Event payload cannot be null");
        }

        // Valida que nenhum campo proibido está presente
        String payloadString = payload.toString().toLowerCase();

        for (String forbiddenField : FORBIDDEN_FIELDS) {
            if (payloadString.contains(forbiddenField.toLowerCase())) {
                throw new IllegalStateException(
                        "Event payload contains forbidden field: " + forbiddenField);
            }
        }

        // Valida IDs não-nulos
        if (payload.watchEntryId() == null) {
            throw new IllegalArgumentException("WatchEntry ID cannot be null in event payload");
        }

        if (payload.userId() == null) {
            throw new IllegalArgumentException("User ID cannot be null in event payload");
        }

        // Valida que pelo menos mediaId ou episodeId está presente
        if (payload.mediaId() == null && payload.episodeId() == null) {
            throw new IllegalArgumentException(
                    "At least one of mediaId or episodeId must be present in event payload");
        }

        // Valida rating range (se presente)
        if (payload.rating() != null) {
            if (payload.rating().compareTo(java.math.BigDecimal.ZERO) < 0
                    || payload.rating().compareTo(new java.math.BigDecimal("10")) > 0) {
                throw new IllegalArgumentException(
                        "Rating must be between 0 and 10, got: " + payload.rating());
            }
        }
    }

    @Override
    public String[] getAllowedFields() {
        return ALLOWED_FIELDS.clone(); // Clone para evitar modificação externa
    }

    /**
     * Retorna lista de campos proibidos para documentação.
     *
     * @return Lista imutável de campos proibidos
     */
    public List<String> getForbiddenFields() {
        return List.copyOf(FORBIDDEN_FIELDS);
    }
}
