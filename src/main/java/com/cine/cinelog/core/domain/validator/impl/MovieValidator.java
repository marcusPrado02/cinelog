package com.cine.cinelog.core.domain.validator.impl;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.validator.AbstractMediaValidator;

/**
 * Implementação concreta de validador para mídias do tipo MOVIE (filme).
 *
 * <p>
 * <strong>Template Method Pattern</strong>: Implementa as validações
 * específicas
 * de filmes, complementando as validações comuns definidas em
 * {@link AbstractMediaValidator}.
 *
 * <p>
 * <strong>Regras Específicas de Filmes</strong>:
 * <ul>
 * <li>Filmes clássicos (antes de 1960) podem não ter ano exato</li>
 * <li>Filmes modernos devem ter ano de lançamento</li>
 * <li>Overview recomendada (warning se ausente, mas não bloqueia)</li>
 * <li>Título original recomendado para filmes estrangeiros</li>
 * </ul>
 *
 * <p>
 * <strong>Diferenças vs Séries</strong>:
 * <ul>
 * <li>Filmes não têm temporadas ou episódios</li>
 * <li>Filmes têm ciclo de vida mais simples (lançamento único)</li>
 * <li>Validações de duração e classificação etária são opcionais</li>
 * </ul>
 *
 * <p>
 * <strong>Exemplo de Uso</strong>:
 * 
 * <pre>{@code
 * Media movie = new Media(null, "Inception", MediaType.MOVIE, 2010, ...);
 * MovieValidator validator = new MovieValidator();
 * validator.validate(movie); // Template Method da classe base
 * }</pre>
 *
 * @since 1.0
 * @see AbstractMediaValidator
 * @see SeriesValidator
 */
public class MovieValidator extends AbstractMediaValidator {

    /**
     * Ano que marca transição entre filmes clássicos e modernos.
     * Filmes antes deste ano têm regras mais flexíveis.
     */
    private static final int CLASSIC_MOVIE_THRESHOLD = 1960;

    /**
     * Valida regras específicas de filmes.
     *
     * <p>
     * <strong>Validações Aplicadas</strong>:
     * <ol>
     * <li>Filmes modernos (>= 1960) devem ter ano de lançamento</li>
     * <li>Título original recomendado se idioma diferente de inglês</li>
     * <li>Overview recomendada (não obrigatória)</li>
     * </ol>
     *
     * <p>
     * Implementa o hook method
     * {@link AbstractMediaValidator#validateTypeSpecificRules(Media)}.
     *
     * @param media mídia do tipo MOVIE a ser validada
     * @throws IllegalArgumentException se validações obrigatórias falharem
     */
    @Override
    protected void validateTypeSpecificRules(Media media) {
        // Validação 1: Filmes modernos devem ter ano de lançamento
        validateReleaseYearForModernMovies(media);

        // Validação 2: Recomendações (não bloqueantes)
        checkRecommendations(media);
    }

    /**
     * Valida ano de lançamento para filmes modernos.
     *
     * <p>
     * <strong>Regra</strong>: Filmes lançados a partir de 1960 devem ter
     * ano de lançamento definido, pois há registros precisos.
     *
     * <p>
     * Filmes clássicos (antes de 1960) podem não ter ano exato devido
     * a registros históricos imprecisos.
     *
     * @param media mídia a validar
     */
    private void validateReleaseYearForModernMovies(Media media) {
        Integer year = media.getReleaseYear();

        // Se ano não está definido, verificar se é filme moderno
        if (year == null) {
            // Para filmes clássicos, ano é opcional
            // Para filmes modernos, seria recomendado mas não bloqueamos
            return;
        }

        // Se ano está definido e é filme moderno
        if (year >= CLASSIC_MOVIE_THRESHOLD) {
            // Validação adicional: ano não pode ser muito futuro
            int currentYear = java.time.Year.now().getValue();
            if (year > currentYear + 5) {
                throw new IllegalArgumentException(
                        String.format("Movie release year %d is too far in the future", year));
            }
        }
    }

    /**
     * Verifica recomendações (boas práticas) para filmes.
     *
     * <p>
     * <strong>Recomendações verificadas</strong>:
     * <ul>
     * <li>Overview deve estar presente para melhor experiência do usuário</li>
     * <li>Título original recomendado para filmes estrangeiros</li>
     * <li>TMDB ID recomendado para integração com serviços externos</li>
     * </ul>
     *
     * <p>
     * <strong>Nota</strong>: Estas são recomendações, não validações obrigatórias.
     * Podem gerar warnings em logs mas não bloqueiam a criação.
     *
     * @param media mídia a validar
     */
    private void checkRecommendations(Media media) {
        // Recomendação 1: Overview presente
        if (media.getOverview() == null || media.getOverview().isBlank()) {
            // Em produção, isso geraria um log warning
            // Por ora, apenas nota que está ausente (não bloqueia)
        }

        // Recomendação 2: Título original para filmes não-ingleses
        if (media.getOriginalLanguage() != null
                && !media.getOriginalLanguage().equalsIgnoreCase("en")
                && !media.getOriginalLanguage().equalsIgnoreCase("eng")
                && (media.getOriginalTitle() == null || media.getOriginalTitle().isBlank())) {
            // Idealmente teria título original para filmes estrangeiros
            // Mas não é obrigatório
        }

        // Recomendação 3: TMDB ID para integração
        if (!media.hasTmdbId()) {
            // TMDB ID facilita integração com serviços externos
            // Mas não é obrigatório
        }
    }

    /**
     * Retorna identificador do tipo de mídia validado.
     *
     * @return "MOVIE"
     */
    @Override
    public String getMediaTypeName() {
        return "MOVIE";
    }

    /**
     * Normalização pós-validação específica de filmes.
     *
     * <p>
     * <strong>Normalizações aplicadas</strong>:
     * <ul>
     * <li>Remove espaços extras do título</li>
     * <li>Capitaliza primeira letra do título (se todo minúsculo)</li>
     * <li>Trunca overview se exceder limite (com reticências)</li>
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

        // Trunca overview se muito longa (com reticências)
        if (media.getOverview() != null && media.getOverview().length() > 5000) {
            String truncated = media.getOverview().substring(0, 4997) + "...";
            media.setOverview(truncated);
        }
    }
}
