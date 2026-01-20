package com.cine.cinelog.core.domain.validator;

import com.cine.cinelog.core.domain.model.Media;

/**
 * Classe abstrata que define o Template Method para validação de mídias.
 *
 * <p>
 * <strong>Template Method Pattern</strong>: Define o esqueleto de um algoritmo
 * em uma operação, delegando alguns passos para subclasses. Permite que
 * subclasses redefinam certas etapas de um algoritmo sem mudar sua estrutura.
 *
 * <p>
 * <strong>Algoritmo de Validação</strong> (ordem fixa):
 * <ol>
 * <li>Validações comuns (título, ano, tipo) -
 * {@link #validateCommonRules(Media)}</li>
 * <li>Validações específicas do tipo -
 * {@link #validateTypeSpecificRules(Media)}</li>
 * <li>Validações de metadados - {@link #validateMetadata(Media)}</li>
 * </ol>
 *
 * <p>
 * <strong>Uso</strong>:
 *
 * <pre>{@code
 * MediaValidator validator = MediaValidatorFactory.getValidator(media.getType());
 * validator.validate(media); // Template Method
 * }</pre>
 *
 * <p>
 * <strong>Implementações</strong>:
 * <ul>
 * <li>{@link MovieValidator} - Regras específicas de filmes</li>
 * <li>{@link SeriesValidator} - Regras específicas de séries</li>
 * </ul>
 *
 * @since 1.0
 * @see MovieValidator
 * @see SeriesValidator
 */
public abstract class AbstractMediaValidator {

    /**
     * Template Method que define o algoritmo de validação.
     *
     * <p>
     * Método final para garantir que a ordem de validação não seja alterada
     * pelas subclasses. Este é o método público chamado pelos clientes.
     *
     * <p>
     * <strong>Fluxo de execução</strong>:
     * <ol>
     * <li>Validação de regras comuns (todos os tipos)</li>
     * <li>Validação de regras específicas do tipo (hook method)</li>
     * <li>Pós-validação e normalização (hook method opcional)</li>
     * <li>Validação de metadados (após normalização)</li>
     * </ol>
     *
     * @param media mídia a ser validada
     * @throws com.cine.cinelog.core.domain.error.DomainException se alguma
     *                                                            validação falhar
     */
    public final void validate(Media media) {
        // Passo 1: Validações comuns a todos os tipos
        validateCommonRules(media);

        // Passo 2: Validações específicas do tipo (hook method)
        // Delegado para subclasses concretas
        validateTypeSpecificRules(media);

        // Passo 3: Pós-validação e normalização (pode truncar overview)
        postValidation(media);

        // Passo 4: Validações de metadados (após normalização)
        validateMetadata(media);
    }

    /**
     * Valida regras comuns a todos os tipos de mídia.
     *
     * <p>
     * Regras validadas:
     * <ul>
     * <li>Título não pode ser nulo ou vazio</li>
     * <li>Tipo deve estar definido</li>
     * <li>Ano de lançamento deve estar entre 1888 e ano atual + 5</li>
     * <li>Título original, se fornecido, não pode ser vazio</li>
     * </ul>
     *
     * <p>
     * Método concreto (não abstrato) porque a lógica é a mesma para
     * todos os tipos de mídia.
     *
     * @param media mídia a validar
     */
    protected void validateCommonRules(Media media) {
        if (media == null) {
            throw new IllegalArgumentException("Media cannot be null");
        }

        // Título é obrigatório
        if (media.getTitle() == null || media.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        // Tipo é obrigatório
        if (media.getType() == null) {
            throw new IllegalArgumentException("Media type is required");
        }

        // Ano de lançamento válido
        if (media.getReleaseYear() != null) {
            int currentYear = java.time.Year.now().getValue();
            int maxYear = currentYear + 5; // Permite filmes futuros (até 5 anos)

            if (media.getReleaseYear() < 1888 || media.getReleaseYear() > maxYear) {
                throw new IllegalArgumentException(
                        String.format("Release year must be between 1888 and %d", maxYear));
            }
        }

        // Título original não pode ser vazio (se fornecido)
        if (media.getOriginalTitle() != null && media.getOriginalTitle().isBlank()) {
            throw new IllegalArgumentException("Original title cannot be blank");
        }
    }

    /**
     * Hook Method: Valida regras específicas do tipo de mídia.
     *
     * <p>
     * <strong>Método abstrato</strong> - Deve ser implementado pelas subclasses
     * para definir validações específicas de cada tipo:
     * <ul>
     * <li>{@link MovieValidator}: valida duração, IMDB rating, etc.</li>
     * <li>{@link SeriesValidator}: valida número de temporadas, status, etc.</li>
     * </ul>
     *
     * <p>
     * Este é o ponto de extensão do Template Method Pattern.
     *
     * @param media mídia a validar
     */
    protected abstract void validateTypeSpecificRules(Media media);

    /**
     * Valida metadados opcionais da mídia.
     *
     * <p>
     * Regras validadas:
     * <ul>
     * <li>URLs (poster, backdrop) devem ser válidas se fornecidas</li>
     * <li>Idioma original deve ter formato válido (2-10 caracteres)</li>
     * <li>Overview não pode exceder 5000 caracteres</li>
     * <li>TMDB ID deve ser positivo se fornecido</li>
     * </ul>
     *
     * <p>
     * Método concreto porque a lógica é comum a todos os tipos.
     *
     * @param media mídia a validar
     */
    protected void validateMetadata(Media media) {
        // Idioma original (se fornecido)
        if (media.getOriginalLanguage() != null) {
            String lang = media.getOriginalLanguage().trim();
            if (lang.length() < 2 || lang.length() > 10) {
                throw new IllegalArgumentException(
                        "Original language must be between 2 and 10 characters");
            }
        }

        // Overview (sinopse)
        if (media.getOverview() != null) {
            if (media.getOverview().length() > 5000) {
                throw new IllegalArgumentException(
                        "Overview cannot exceed 5000 characters");
            }
        }

        // TMDB ID (se fornecido)
        if (media.getTmdbId() != null && media.getTmdbId() <= 0) {
            throw new IllegalArgumentException("TMDB ID must be positive");
        }

        // URLs são validadas pela MediaPolicy
        // Aqui apenas verificamos que não são vazias se fornecidas
        if (media.getPosterUrl() != null && media.getPosterUrl().isBlank()) {
            throw new IllegalArgumentException("Poster URL cannot be blank");
        }

        if (media.getBackdropUrl() != null && media.getBackdropUrl().isBlank()) {
            throw new IllegalArgumentException("Backdrop URL cannot be blank");
        }
    }

    /**
     * Retorna o nome do tipo de mídia que este validator valida.
     *
     * <p>
     * Hook Method para identificação do validator.
     * Útil para logging e debugging.
     *
     * @return nome do tipo (ex: "MOVIE", "SERIES")
     */
    public abstract String getMediaTypeName();

    /**
     * Hook Method: Permite subclasses adicionarem lógica de pós-validação.
     *
     * <p>
     * Implementação padrão vazia - subclasses podem sobrescrever se necessário.
     * Exemplo: normalizar dados após validação bem-sucedida.
     *
     * @param media mídia validada
     */
    protected void postValidation(Media media) {
        // Hook opcional - implementação padrão vazia
        // Subclasses podem sobrescrever para adicionar lógica adicional
    }
}
