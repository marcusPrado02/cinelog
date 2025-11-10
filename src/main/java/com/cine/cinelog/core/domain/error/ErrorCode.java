package com.cine.cinelog.core.domain.error;

/**
 * Catálogo padronizado de códigos de erro.
 * Formato sugerido: {AREA}-{TIPO}-{NÚMERO}
 */
public enum ErrorCode {

    // GENÉRICOS
    GEN_UNEXPECTED("GEN-000", "Erro inesperado"),
    GEN_VALIDATION("GEN-001", "Falha de validação"),
    GEN_CONSTRAINT("GEN-002", "Restrição de integridade violada"),
    GEN_NOT_FOUND("GEN-003", "Recurso não encontrado"),
    GEN_CONFLICT("GEN-004", "Conflito de versão ou estado"),
    OPERATION_NOT_ALLOWED("GEN-005", "Operação não permitida"),

    // DOMÍNIO: MEDIA
    MEDIA_NOT_FOUND("MEDIA-404", "Mídia não encontrada"),
    MEDIA_DUPLICATE("MEDIA-409", "Mídia duplicada"),
    MEDIA_TITLE_REQUIRED("MEDIA-001", "Título é obrigatório"),
    MEDIA_YEAR_OUT_OF_RANGE("MEDIA-002", "Ano fora do intervalo"),
    MEDIA_TITLE_TOO_LONG("MEDIA-003", "Título excede 200 caracteres"),
    RATING_OUT_OF_RANGE("RATING-001", "Classificação fora do intervalo"),
    RATING_NOT_ALLOWED("RATING-002", "Classificação não permitida"),
    INVALID_RELEASE_YEAR("RATING-003", "Ano de lançamento inválido"),
    INVALID_WATCH_ENTRY("RATING-004", "Entrada de assistido inválida"),
    INVALID_ARGUMENT("RATING-005", "Argumento inválido"),

    // DOMÍNIO: USER
    USER_NOT_FOUND("USER-404", "Usuário não encontrado"),
    USER_EMAIL_INVALID("USER-001", "E-mail inválido");

    public final String code;
    public final String title;

    ErrorCode(String code, String title) {
        this.code = code;
        this.title = title;
    }
}