package com.cine.cinelog.core.application.events.mappers;

/**
 * Interface para mapeamento seguro de payloads de eventos.
 * <p>
 * Implementações devem garantir:
 * - Apenas campos allowlisted são incluídos
 * - Dados sensíveis (email, password, token) são excluídos
 * - Campos livres (comentários, etc.) são truncados
 * <p>
 * PR5: Event Security - PII Protection
 *
 * @param <T> Tipo do domain model
 * @param <R> Tipo do event payload (DTO seguro)
 */
public interface EventPayloadMapper<T, R> {

    /**
     * Mapeia domain model para event payload seguro.
     * <p>
     * Deve aplicar:
     * - Allowlist de campos
     * - Redaction de dados sensíveis
     * - Truncamento de campos livres
     *
     * @param domainModel Modelo de domínio original
     * @return Payload seguro para publicação
     */
    R toEventPayload(T domainModel);

    /**
     * Valida se o payload está de acordo com as regras de segurança.
     * <p>
     * Lança exceção se:
     * - Contiver campos proibidos (email, password, token, secret)
     * - Campos livres excederem tamanho máximo
     *
     * @param payload Payload a validar
     * @throws IllegalArgumentException se payload contiver dados sensíveis
     */
    default void validatePayload(R payload) {
        // Implementação padrão vazia - sobrescrever se necessário
    }

    /**
     * Retorna a lista de campos permitidos (allowlist).
     * <p>
     * Usado para documentação e validação.
     *
     * @return Array de nomes de campos permitidos
     */
    String[] getAllowedFields();
}
