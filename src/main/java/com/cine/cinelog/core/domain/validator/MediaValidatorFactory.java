package com.cine.cinelog.core.domain.validator;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.validator.impl.MovieValidator;
import com.cine.cinelog.core.domain.validator.impl.SeriesValidator;

/**
 * Factory para criação de validadores de mídia específicos por tipo.
 *
 * <p>
 * <strong>Design Pattern: Factory Method</strong>
 *
 * <p>
 * Esta factory encapsula a lógica de seleção do validador apropriado baseado
 * no tipo de mídia (MOVIE ou SERIES), aplicando o padrão Template Method
 * através das implementações concretas.
 *
 * <p>
 * <strong>Arquitetura</strong>:
 * 
 * <pre>
 * MediaValidatorFactory
 *      |
 *      +---> getValidator(MediaType) --> AbstractMediaValidator
 *                                              |
 *                                              +---> MovieValidator
 *                                              +---> SeriesValidator
 * </pre>
 *
 * <p>
 * <strong>Benefícios</strong>:
 * <ul>
 * <li>Desacoplamento: Clientes não precisam conhecer classes concretas</li>
 * <li>Extensibilidade: Novos tipos de mídia adicionados facilmente</li>
 * <li>Validação centralizada: Ponto único de acesso aos validadores</li>
 * <li>Type-Safe: Validação em tempo de compilação</li>
 * </ul>
 *
 * <p>
 * <strong>Exemplo de Uso</strong>:
 * 
 * <pre>{@code
 * Media movie = new Media(null, "Inception", MediaType.MOVIE, 2010, ...);
 * AbstractMediaValidator validator = MediaValidatorFactory.getValidator(movie.getType());
 * validator.validate(movie); // Usa MovieValidator
 *
 * Media series = new Media(null, "Breaking Bad", MediaType.SERIES, 2008, ...);
 * AbstractMediaValidator validator = MediaValidatorFactory.getValidator(series.getType());
 * validator.validate(series); // Usa SeriesValidator
 * }</pre>
 *
 * <p>
 * <strong>Extensão Futura</strong>:
 * Para adicionar novo tipo de mídia (ex: DOCUMENTARY):
 * <ol>
 * <li>Adicionar valor ao enum {@link MediaType}</li>
 * <li>Criar classe
 * {@code DocumentaryValidator extends AbstractMediaValidator}</li>
 * <li>Adicionar case no switch de {@link #getValidator(MediaType)}</li>
 * </ol>
 *
 * @since 1.0
 * @see AbstractMediaValidator
 * @see MovieValidator
 * @see SeriesValidator
 * @see MediaType
 */
public final class MediaValidatorFactory {

    /**
     * Instância singleton do validador de filmes.
     * Stateless, pode ser reutilizada com segurança.
     */
    private static final AbstractMediaValidator MOVIE_VALIDATOR = new MovieValidator();

    /**
     * Instância singleton do validador de séries.
     * Stateless, pode ser reutilizada com segurança.
     */
    private static final AbstractMediaValidator SERIES_VALIDATOR = new SeriesValidator();

    /**
     * Construtor privado para prevenir instanciação.
     * Factory com métodos estáticos.
     */
    private MediaValidatorFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }

    /**
     * Retorna o validador apropriado para o tipo de mídia especificado.
     *
     * <p>
     * <strong>Implementação</strong>: Usa pattern matching do Java 21 para
     * seleção eficiente do validador. Validadores são singletons pois não
     * mantêm estado (stateless).
     *
     * @param type tipo da mídia (MOVIE ou SERIES)
     * @return validador específico para o tipo
     * @throws IllegalArgumentException se tipo for null ou não suportado
     */
    public static AbstractMediaValidator getValidator(MediaType type) {
        if (type == null) {
            throw new IllegalArgumentException("MediaType cannot be null");
        }

        return switch (type) {
            case MOVIE -> MOVIE_VALIDATOR;
            case SERIES -> SERIES_VALIDATOR;
            // Futuros tipos de mídia aqui:
            // case DOCUMENTARY -> DOCUMENTARY_VALIDATOR;
        };
    }

    /**
     * Retorna o validador apropriado baseado no objeto Media.
     *
     * <p>
     * Método de conveniência que extrai o tipo da mídia e delega para
     * {@link #getValidator(MediaType)}.
     *
     * @param media mídia para qual obter o validador
     * @return validador específico para o tipo da mídia
     * @throws IllegalArgumentException se media for null ou tipo não definido
     */
    public static AbstractMediaValidator getValidator(Media media) {
        if (media == null) {
            throw new IllegalArgumentException("Media cannot be null");
        }

        if (media.getType() == null) {
            throw new IllegalArgumentException("Media type cannot be null");
        }

        return getValidator(media.getType());
    }

    /**
     * Valida uma mídia usando o validador apropriado para seu tipo.
     *
     * <p>
     * Método de conveniência que combina a obtenção do validador e a validação
     * em uma única chamada. Útil para casos de uso simples.
     *
     * <p>
     * <strong>Exemplo</strong>:
     * 
     * <pre>{@code
     * Media movie = new Media(...);
     * MediaValidatorFactory.validate(movie); // Detecta tipo e valida
     * }</pre>
     *
     * @param media mídia a ser validada
     * @throws IllegalArgumentException se validação falhar ou parâmetros inválidos
     */
    public static void validate(Media media) {
        AbstractMediaValidator validator = getValidator(media);
        validator.validate(media);
    }

    /**
     * Verifica se o tipo de mídia é suportado pela factory.
     *
     * <p>
     * Útil para validações preventivas antes de tentar obter um validador.
     *
     * @param type tipo de mídia a verificar
     * @return true se o tipo é suportado, false caso contrário
     */
    public static boolean isSupported(MediaType type) {
        if (type == null) {
            return false;
        }

        return type == MediaType.MOVIE || type == MediaType.SERIES;
        // Futuros tipos adicionados aqui:
        // || type == MediaType.DOCUMENTARY;
    }
}
