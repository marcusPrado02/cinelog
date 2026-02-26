package com.cine.cinelog.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validador de limites de negócio para prevenção de abuso (A04 — OWASP).
 *
 * <h3>O que são limites de negócio?</h3>
 * <p>
 * São restrições que fazem sentido no <b>contexto do domínio</b>, não apenas
 * no nível técnico. Rate limiting controla "quantas requests por tempo";
 * business limits controlam "quantos recursos por entidade".
 * </p>
 *
 * <h3>Qual a diferença de Rate Limiting?</h3>
 * <table>
 * <tr>
 * <th>Aspecto</th>
 * <th>Rate Limiting</th>
 * <th>Business Limits</th>
 * </tr>
 * <tr>
 * <td>Controla</td>
 * <td>Requests por tempo</td>
 * <td>Recursos por entidade</td>
 * </tr>
 * <tr>
 * <td>Exemplo</td>
 * <td>100 requests/min</td>
 * <td>200 reviews/dia/usuário</td>
 * </tr>
 * <tr>
 * <td>Camada</td>
 * <td>Filtro HTTP (infra)</td>
 * <td>Service layer (domínio)</td>
 * </tr>
 * </table>
 *
 * <p>
 * Um atacante pode respeitar o rate limit (1 req/segundo) mas criar
 * 86.400 reviews por dia — sem business limits, cada request é "válida".
 * </p>
 *
 * <h3>Cenário de ataque prevenido (Resource Exhaustion)</h3>
 * <p>
 * Atacante cria conta gratuita e usa script para cadastrar milhões de
 * registros,
 * enchendo o banco de dados e degradando performance para todos os usuários.
 * </p>
 *
 * @since 1.1
 * @see BusinessLimitExceededException
 */
@Component
@Slf4j
public class BusinessLimitValidator {

    /** Máximo de reviews que um usuário pode criar por dia. */
    public static final int MAX_REVIEWS_PER_DAY = 200;

    /** Máximo de itens na watchlist de um usuário. */
    public static final int MAX_WATCHLIST_SIZE = 1000;

    /** Máximo de itens em operação de bulk/batch. */
    public static final int MAX_BULK_SIZE = 100;

    /** Tamanho máximo de upload de imagem (5 MB). */
    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    /**
     * Valida se um contador está dentro do limite permitido.
     *
     * @param current  valor atual (ex: reviews criadas hoje pelo usuário)
     * @param limit    limite máximo
     * @param resource nome do recurso (para mensagem de erro e logging)
     * @throws BusinessLimitExceededException se o limite foi atingido
     */
    public void validateLimit(long current, long limit, String resource) {
        if (current >= limit) {
            log.warn("A04-BusinessLimit: Limite excedido — resource={}, current={}, limit={}",
                    resource, current, limit);
            throw new BusinessLimitExceededException(
                    String.format("Limite de %s atingido (%d/%d). Tente novamente mais tarde.",
                            resource, current, limit));
        }
    }

    /**
     * Valida tamanho de coleção/batch para operações em lote.
     *
     * @param size      tamanho da coleção enviada
     * @param maxSize   tamanho máximo permitido
     * @param operation nome da operação (para logging)
     * @throws BusinessLimitExceededException se excedeu o tamanho
     */
    public void validateBatchSize(int size, int maxSize, String operation) {
        if (size > maxSize) {
            log.warn("A04-BusinessLimit: Batch excedido — operation={}, size={}, max={}",
                    operation, size, maxSize);
            throw new BusinessLimitExceededException(
                    String.format("Operação '%s' limitada a %d itens por vez. Enviados: %d.",
                            operation, maxSize, size));
        }
    }

    /**
     * Valida tamanho de arquivo (ex: upload de imagem).
     *
     * @param sizeBytes tamanho em bytes
     * @param maxBytes  limite máximo em bytes
     * @param fileType  tipo do arquivo (para mensagem de erro)
     * @throws BusinessLimitExceededException se excedeu o limite
     */
    public void validateFileSize(long sizeBytes, long maxBytes, String fileType) {
        if (sizeBytes > maxBytes) {
            long maxMB = maxBytes / (1024 * 1024);
            throw new BusinessLimitExceededException(
                    String.format("Arquivo %s excede o tamanho máximo de %d MB.", fileType, maxMB));
        }
    }
}
