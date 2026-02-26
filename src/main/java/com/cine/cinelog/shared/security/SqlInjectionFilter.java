package com.cine.cinelog.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Filtro HTTP que detecta padrões de SQL Injection em parâmetros de query
 * string.
 *
 * <h3>A03 (OWASP) — Camada de defesa em profundidade</h3>
 *
 * <p>
 * Embora o JPA/Hibernate use Prepared Statements (proteção primária),
 * este filtro atua como <b>segunda camada</b> bloqueando requisições
 * maliciosas antes mesmo de chegarem ao controller.
 * </p>
 *
 * <h3>O que verifica</h3>
 * <ul>
 * <li>Todos os parâmetros de query string (ex.:
 * {@code ?title=...&year=...})</li>
 * <li>Padrões detectados: UNION SELECT, DROP TABLE, tautologias, comentários
 * SQL,
 * SLEEP/BENCHMARK (blind injection), xp_cmdshell, etc.</li>
 * </ul>
 *
 * <h3>O que NÃO verifica (e por quê)</h3>
 * <ul>
 * <li><b>Body JSON</b>: deserializado pelo Jackson com tipos fortes
 * (DTOs com validação Bean Validation), não interpolado em SQL</li>
 * <li><b>Headers</b>: tratados pelo Spring Security (JWT, CORS, etc.)</li>
 * <li><b>Paths estáticos</b>: Swagger, actuator e docs são ignorados
 * para não gerar falsos positivos</li>
 * </ul>
 *
 * <h3>Fluxo ao detectar ataque</h3>
 * <ol>
 * <li>Log WARN com IP, URI, parâmetro e valor (sanitizados)</li>
 * <li>Incrementa métrica de tentativa (via logs estruturados)</li>
 * <li>Retorna HTTP 400 Bad Request com mensagem genérica</li>
 * <li>NÃO revela qual padrão foi detectado (para não ajudar atacante)</li>
 * </ol>
 *
 * <h3>Posição na cadeia de filtros</h3>
 * <p>
 * Registrado <b>antes</b> do {@link JwtAuthenticationFilter} no
 * {@code SecurityFilterChain}, para bloquear payloads maliciosos antes
 * de qualquer processamento de autenticação.
 * </p>
 *
 * @since 1.1
 * @see InputSanitizer#containsSqlPattern(String)
 * @see com.cine.cinelog.shared.config.SecurityConfig
 */
@Slf4j
public class SqlInjectionFilter extends OncePerRequestFilter {

    /**
     * Paths ignorados pelo filtro (conteúdo estático e documentação).
     * Não precisam de verificação SQL pois não interagem com banco de dados.
     */
    private static final Set<String> SKIP_PATHS = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Pular paths que não interagem com banco
        if (shouldSkip(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Verificar todos os parâmetros da query string
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                if (InputSanitizer.containsSqlPattern(value)) {
                    log.warn(
                            "A03-SQLi: Padrão de SQL Injection detectado. "
                                    + "IP={}, URI={}, param={}, valor={}",
                            InputSanitizer.sanitizeForLog(request.getRemoteAddr()),
                            InputSanitizer.sanitizeForLog(uri),
                            InputSanitizer.sanitizeForLog(entry.getKey()),
                            InputSanitizer.sanitizeForLog(value));

                    rejectRequest(response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Verifica se o path deve ser ignorado pelo filtro.
     */
    private boolean shouldSkip(String uri) {
        return SKIP_PATHS.stream().anyMatch(uri::startsWith);
    }

    /**
     * Retorna HTTP 400 com corpo JSON genérico.
     * <p>
     * A mensagem é propositalmente vaga — não informa qual padrão
     * foi detectado para não dar pistas ao atacante.
     * </p>
     */
    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"type\":\"about:blank\","
                        + "\"title\":\"Bad Request\","
                        + "\"status\":400,"
                        + "\"detail\":\"A requisição contém caracteres inválidos.\"}");
    }
}
