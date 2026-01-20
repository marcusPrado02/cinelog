package com.cine.cinelog.core.domain.error;

/**
 * Catálogo padronizado de códigos de erro.
 * Formato sugerido: {AREA}-{TIPO}-{NÚMERO}
 *
 * Importante:
 * - code: usado para logs, monitoramento e identificação estável do erro.
 * - title: mensagem padrão (e fallback) em português.
 * - messageKey: chave base de i18n (ex.: "error.media.title.required").
 * O handler derivará:
 * - {messageKey}.title
 * - {messageKey}.detail
 */
public enum ErrorCode {

        // GENÉRICOS
        GEN_UNEXPECTED(
                        "GEN-000",
                        "Erro inesperado",
                        "error.gen.unexpected"),
        GEN_VALIDATION(
                        "GEN-001",
                        "Falha de validação",
                        "error.gen.validation"),
        GEN_CONSTRAINT(
                        "GEN-002",
                        "Restrição de integridade violada",
                        "error.gen.constraint"),
        GEN_NOT_FOUND(
                        "GEN-003",
                        "Recurso não encontrado",
                        "error.gen.notFound"),
        GEN_CONFLICT(
                        "GEN-004",
                        "Conflito de versão ou estado",
                        "error.gen.conflict"),
        OPERATION_NOT_ALLOWED(
                        "GEN-005",
                        "Operação não permitida",
                        "error.gen.operationNotAllowed"),

        // DOMÍNIO: MEDIA
        MEDIA_NOT_FOUND(
                        "MEDIA-404",
                        "Mídia não encontrada",
                        "error.media.notFound"),
        MEDIA_DUPLICATE(
                        "MEDIA-409",
                        "Mídia duplicada",
                        "error.media.duplicate"),
        MEDIA_TITLE_REQUIRED(
                        "MEDIA-001",
                        "Título é obrigatório",
                        "error.media.title.required"),
        MEDIA_YEAR_OUT_OF_RANGE(
                        "MEDIA-002",
                        "Ano fora do intervalo",
                        "error.media.year.outOfRange"),
        MEDIA_TITLE_TOO_LONG(
                        "MEDIA-003",
                        "Título excede o tamanho máximo permitido",
                        "error.media.title.tooLong"),
        MEDIA_TYPE_REQUIRED(
                        "MEDIA-004",
                        "Tipo de mídia é obrigatório",
                        "error.media.type.required"),
        MEDIA_ORIGINAL_LANGUAGE_INVALID(
                        "MEDIA-005",
                        "Idioma original inválido",
                        "error.media.originalLanguage.invalid"),
        MEDIA_OVERVIEW_TOO_LONG(
                        "MEDIA-006",
                        "Sinopse excede o tamanho máximo permitido",
                        "error.media.overview.tooLong"),
        MEDIA_POSTER_URL_INVALID(
                        "MEDIA-007",
                        "URL do pôster inválida",
                        "error.media.posterUrl.invalid"),
        MEDIA_BACKDROP_URL_INVALID(
                        "MEDIA-008",
                        "URL do backdrop inválida",
                        "error.media.backdropUrl.invalid"),
        MEDIA_YEAR_REQUIRED_FOR_SERIES(
                        "MEDIA-009",
                        "Ano de lançamento obrigatório para séries",
                        "error.media.year.requiredForSeries"),
        MEDIA_TYPE_IMMUTABLE_WITH_HISTORY(
                        "MEDIA-010",
                        "Não é permitido alterar o tipo de mídia com histórico associado",
                        "error.media.type.immutableWithHistory"),
        MEDIA_DELETE_FORBIDDEN(
                        "MEDIA-011",
                        "Não é permitido excluir esta mídia",
                        "error.media.delete.forbidden"),

        // DOMÍNIO: RATING / WATCH ENTRY
        RATING_OUT_OF_RANGE(
                        "RATING-001",
                        "Classificação fora do intervalo",
                        "error.rating.outOfRange"),
        RATING_NOT_ALLOWED(
                        "RATING-002",
                        "Classificação não permitida",
                        "error.rating.notAllowed"),
        INVALID_RELEASE_YEAR(
                        "RATING-003",
                        "Ano de lançamento inválido",
                        "error.rating.invalidReleaseYear"),
        INVALID_WATCH_ENTRY(
                        "RATING-004",
                        "Entrada de assistido inválida",
                        "error.watchEntry.invalid"),
        INVALID_ARGUMENT(
                        "RATING-005",
                        "Argumento inválido",
                        "error.argument.invalid"),

        WATCH_ENTRY_DATE_IN_FUTURE(
                        "WATCH-001",
                        "Data de assistido não pode ser futura",
                        "error.watchEntry.date.inFuture"),
        WATCH_ENTRY_COMMENT_TOO_LONG(
                        "WATCH-002",
                        "Comentário excede o tamanho máximo permitido",
                        "error.watchEntry.comment.tooLong"),
        WATCH_ENTRY_RATING_WITHOUT_DATE(
                        "WATCH-003",
                        "Não é permitido avaliar sem data de assistido",
                        "error.watchEntry.ratingWithoutDate"),
        WATCH_ENTRY_DUPLICATE(
                        "WATCH-004",
                        "Entrada de assistido duplicada",
                        "error.watchEntry.duplicate"),

        // DOMÍNIO: WATCH ENTRY STATE
        INVALID_STATE_TRANSITION(
                        "WATCH-005",
                        "Transição de estado inválida",
                        "error.watchEntry.state.invalidTransition"),
        INVALID_RATING(
                        "WATCH-006",
                        "Rating inválido para o estado atual",
                        "error.watchEntry.rating.invalid"),

        // DOMÍNIO: WATCHLIST
        WATCHLIST_MEDIA_TYPE_NOT_ALLOWED(
                        "WL-005",
                        "Tipo de mídia não permitido na watchlist",
                        "error.watchlist.mediaType.notAllowed"),
        WATCHLIST_MEDIA_RELEASE_YEAR_INVALID(
                        "WL-006",
                        "Ano de lançamento da mídia não permite inclusão na watchlist",
                        "error.watchlist.media.releaseYear.invalid"),
        WATCHLIST_IMMUTABLE_USER(
                        "WL-007",
                        "Não é permitido alterar o usuário do item de watchlist",
                        "error.watchlist.immutable.user"),
        WATCHLIST_IMMUTABLE_MEDIA(
                        "WL-008",
                        "Não é permitido alterar a mídia do item de watchlist",
                        "error.watchlist.immutable.media"),

        // DOMÍNIO: USER
        USER_NOT_FOUND(
                        "USER-404",
                        "Usuário não encontrado",
                        "error.user.notFound"),
        USER_INVALID(
                        "USER-000",
                        "Usuário inválido",
                        "error.user.invalid"),
        USER_EMAIL_INVALID(
                        "USER-001",
                        "E-mail inválido",
                        "error.user.email.invalid"),
        USER_EMAIL_DUPLICATE(
                        "USER-002",
                        "E-mail já está em uso",
                        "error.user.email.duplicate"),
        USER_NAME_INVALID(
                        "USER-003",
                        "Nome de usuário inválido",
                        "error.user.name.invalid"),
        USER_LOCALE_INVALID(
                        "USER-004",
                        "Locale inválido",
                        "error.user.locale.invalid"),
        USER_EMAIL_IMMUTABLE(
                        "USER-005",
                        "Não é permitido alterar e-mail após verificação",
                        "error.user.email.immutable"),
        USER_DELETE_FORBIDDEN(
                        "USER-006",
                        "Não é permitido excluir este usuário",
                        "error.user.delete.forbidden"),

        // DOMÍNIO: SEASON
        SEASON_INVALID(
                        "SEASON-000",
                        "Temporada inválida",
                        "error.season.invalid"),
        SEASON_NUMBER_INVALID(
                        "SEASON-001",
                        "Número da temporada inválido",
                        "error.season.number.invalid"),
        SEASON_DUPLICATE(
                        "SEASON-002",
                        "Já existe temporada com esse número para esta mídia",
                        "error.season.duplicate"),
        SEASON_DELETE_FORBIDDEN(
                        "SEASON-003",
                        "Não é permitido excluir esta temporada",
                        "error.season.delete.forbidden"),
        SEASON_NOT_FOUND(
                        "SEASON-404",
                        "Temporada não encontrada",
                        "error.season.notFound"),

        // DOMÍNIO: EPISODE
        EPISODE_INVALID(
                        "EP-000",
                        "Episódio inválido",
                        "error.episode.invalid"),
        EPISODE_NUMBER_INVALID(
                        "EP-001",
                        "Número do episódio inválido",
                        "error.episode.number.invalid"),
        EPISODE_DUPLICATE(
                        "EP-002",
                        "Já existe episódio com esse número nesta temporada",
                        "error.episode.duplicate"),
        EPISODE_DELETE_FORBIDDEN(
                        "EP-003",
                        "Não é permitido excluir este episódio",
                        "error.episode.delete.forbidden");

        /**
         * Código estável do erro (para logs, tracing, front).
         */
        public final String code;

        /**
         * Mensagem padrão (fallback), em português.
         * Continua sendo usada como antes e serve como defaultTitle/defaultDetail.
         */
        public final String title;

        /**
         * Chave base de mensagem para i18n (messages_xx.properties).
         * O handler derivará:
         * - {messageKey}.title
         * - {messageKey}.detail
         */
        public final String messageKey;

        ErrorCode(String code, String title, String messageKey) {
                this.code = code;
                this.title = title;
                this.messageKey = messageKey;
        }
}
