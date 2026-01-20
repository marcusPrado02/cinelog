package com.cine.cinelog.core.domain.validator.impl;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.validator.AbstractMediaValidator;

/**
 * Implementação concreta de validador para mídias do tipo SERIES (série de TV).
 *
 * <p>
 * <strong>Template Method Pattern</strong>: Implementa as validações
 * específicas
 * de séries de TV, complementando as validações comuns definidas em
 * {@link AbstractMediaValidator}.
 *
 * <p>
 * <strong>Regras Específicas de Séries</strong>:
 * <ul>
 * <li>Séries DEVEM ter ano de lançamento (início da exibição)</li>
 * <li>Ano deve ser razoável (não muito distante no passado)</li>
 * <li>Overview é altamente recomendada (séries têm enredos complexos)</li>
 * <li>Título original obrigatório para séries estrangeiras</li>
 * </ul>
 *
 * <p>
 * <strong>Diferenças vs Filmes</strong>:
 * <ul>
 * <li>Séries têm temporadas e episódios (validados separadamente)</li>
 * <li>Séries têm ciclo de vida contínuo (podem estar em exibição)</li>
 * <li>Ano de lançamento é obrigatório (marca início da série)</li>
 * <li>Overview é mais importante (enredo de longo prazo)</li>
 * </ul>
 *
 * <p>
 * <strong>Validações Futuras</strong> (em outras camadas):
 * <ul>
 * <li>Número mínimo/máximo de temporadas</li>
 * <li>Validação de status (em exibição, cancelada, finalizada)</li>
 * <li>Data de término (se série finalizada)</li>
 * </ul>
 *
 * <p>
 * <strong>Exemplo de Uso</strong>:
 * 
 * <pre>{@code
 * Media series = new Media(null, "Breaking Bad", MediaType.SERIES, 2008, ...);
 * SeriesValidator validator = new SeriesValidator();
 * validator.validate(series); // Template Method da classe base
 * }</pre>
 *
 * @since 1.0
 * @see AbstractMediaValidator
 * @see MovieValidator
 */
public class SeriesValidator extends AbstractMediaValidator {

    /**
     * Ano mínimo razoável para séries de TV.
     * Considera o início da TV comercial nos anos 1950.
     */
    private static final int MIN_REASONABLE_YEAR_FOR_SERIES = 1950;

    /**
     * Comprimento mínimo recomendado para overview de séries.
     * Séries têm enredos complexos que requerem descrição detalhada.
     */
    private static final int MIN_RECOMMENDED_OVERVIEW_LENGTH = 50;

    /**
     * Valida regras específicas de séries de TV.
     *
     * <p>
     * <strong>Validações Aplicadas</strong>:
     * <ol>
     * <li>Ano de lançamento é OBRIGATÓRIO para séries</li>
     * <li>Ano deve ser razoável (>= 1950)</li>
     * <li>Overview recomendada (maior que 50 caracteres)</li>
     * <li>Título original obrigatório se idioma não for inglês</li>
     * </ol>
     *
     * <p>
     * Implementa o hook method
     * {@link AbstractMediaValidator#validateTypeSpecificRules(Media)}.
     *
     * @param media mídia do tipo SERIES a ser validada
     * @throws IllegalArgumentException se validações obrigatórias falharem
     */
    @Override
    protected void validateTypeSpecificRules(Media media) {
        // Validação 1: Ano obrigatório para séries
        validateReleaseYearRequired(media);

        // Validação 2: Ano deve ser razoável
        validateReasonableYear(media);

        // Validação 3: Overview recomendada
        validateOverviewRecommendation(media);

        // Validação 4: Título original para séries estrangeiras
        validateOriginalTitleForForeignSeries(media);
    }

    /**
     * Valida que ano de lançamento é obrigatório para séries.
     *
     * <p>
     * <strong>Justificativa</strong>: Diferente de filmes clássicos, séries de TV
     * têm registros precisos desde o início da TV comercial. O ano marca o início
     * da exibição da série.
     *
     * @param media mídia a validar
     * @throws IllegalArgumentException se ano não fornecido
     */
    private void validateReleaseYearRequired(Media media) {
        if (media.getReleaseYear() == null) {
            throw new IllegalArgumentException(
                    "Release year is required for TV series");
        }
    }

    /**
     * Valida que ano de lançamento está em intervalo razoável.
     *
     * <p>
     * <strong>Regras</strong>:
     * <ul>
     * <li>Ano mínimo: 1950 (início da TV comercial)</li>
     * <li>Ano máximo: ano atual + 3 (séries anunciadas com antecedência)</li>
     * </ul>
     *
     * @param media mídia a validar
     * @throws IllegalArgumentException se ano fora do intervalo razoável
     */
    private void validateReasonableYear(Media media) {
        int year = media.getReleaseYear(); // Já validado como não-null
        int currentYear = java.time.Year.now().getValue();
        int maxYear = currentYear + 3; // Séries podem ser anunciadas com antecedência

        if (year < MIN_REASONABLE_YEAR_FOR_SERIES) {
            throw new IllegalArgumentException(
                    String.format(
                            "Release year %d is too old for a TV series. Minimum: %d",
                            year, MIN_REASONABLE_YEAR_FOR_SERIES));
        }

        if (year > maxYear) {
            throw new IllegalArgumentException(
                    String.format(
                            "Release year %d is too far in the future. Maximum: %d",
                            year, maxYear));
        }
    }

    /**
     * Valida que overview tem tamanho adequado (recomendação).
     *
     * <p>
     * <strong>Justificativa</strong>: Séries têm enredos complexos que se
     * desenvolvem ao longo de temporadas. Uma overview detalhada ajuda
     * usuários a entender a premissa da série.
     *
     * <p>
     * <strong>Nota</strong>: Esta é uma recomendação forte mas não obrigatória.
     * Séries sem overview adequada são aceitas mas podem gerar warnings.
     *
     * @param media mídia a validar
     */
    private void validateOverviewRecommendation(Media media) {
        String overview = media.getOverview();

        if (overview == null || overview.isBlank()) {
            // Overview ausente - seria ideal ter mas não bloqueamos
            // Em produção, geraria log warning
            return;
        }

        if (overview.trim().length() < MIN_RECOMMENDED_OVERVIEW_LENGTH) {
            // Overview muito curta para série
            // Recomendamos descrição mais detalhada
            // Mas não bloqueamos (apenas log warning em produção)
        }
    }

    /**
     * Valida título original para séries estrangeiras.
     *
     * <p>
     * <strong>Regra</strong>: Séries com idioma original diferente de inglês
     * DEVEM ter título original definido. Isso facilita:
     * <ul>
     * <li>Busca por usuários que conhecem o título original</li>
     * <li>Integração com APIs externas (TMDB, etc.)</li>
     * <li>Identificação única da série</li>
     * </ul>
     *
     * <p>
     * <strong>Exceção</strong>: Séries em inglês podem ter título original
     * igual ao título principal (opcional).
     *
     * @param media mídia a validar
     * @throws IllegalArgumentException se série estrangeira sem título original
     */
    private void validateOriginalTitleForForeignSeries(Media media) {
        String language = media.getOriginalLanguage();

        // Se idioma não definido ou é inglês, título original é opcional
        if (language == null
                || language.equalsIgnoreCase("en")
                || language.equalsIgnoreCase("eng")
                || language.equalsIgnoreCase("english")) {
            return;
        }

        // Para idiomas não-ingleses, título original é obrigatório
        String originalTitle = media.getOriginalTitle();
        if (originalTitle == null || originalTitle.isBlank()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Original title is required for non-English TV series. Language: %s",
                            language));
        }
    }

    /**
     * Retorna identificador do tipo de mídia validado.
     *
     * @return "SERIES"
     */
    @Override
    public String getMediaTypeName() {
        return "SERIES";
    }

    /**
     * Normalização pós-validação específica de séries.
     *
     * <p>
     * <strong>Normalizações aplicadas</strong>:
     * <ul>
     * <li>Remove espaços extras do título</li>
     * <li>Garante título original se idioma for estrangeiro</li>
     * <li>Normaliza idioma original (lowercase)</li>
     * <li>Trunca overview se exceder limite</li>
     * </ul>
     *
     * <p>
     * Sobrescreve o hook method
     * {@link AbstractMediaValidator#postValidation(Media)}.
     *
     * @param media mídia validada a normalizar
     */
    @Override
    protected void postValidation(Media media) {
        // Normaliza título (remove espaços extras)
        if (media.getTitle() != null) {
            String normalized = media.getTitle().trim().replaceAll("\\s+", " ");
            media.setTitle(normalized);
        }

        // Normaliza título original (se presente)
        if (media.getOriginalTitle() != null) {
            String normalized = media.getOriginalTitle().trim().replaceAll("\\s+", " ");
            media.setOriginalTitle(normalized);
        }

        // Normaliza idioma (lowercase)
        if (media.getOriginalLanguage() != null) {
            String normalized = media.getOriginalLanguage().trim().toLowerCase();
            media.setOriginalLanguage(normalized);
        }

        // Trunca overview se muito longa (com reticências)
        if (media.getOverview() != null && media.getOverview().length() > 5000) {
            String truncated = media.getOverview().substring(0, 4997) + "...";
            media.setOverview(truncated);
        }
    }
}
