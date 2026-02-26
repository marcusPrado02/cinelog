package com.cine.cinelog.shared.security;

import java.util.regex.Pattern;

/**
 * Utilitário centralizado de sanitização de entrada para proteção contra
 * injeção (A03 — OWASP).
 *
 * <h3>Camadas de proteção</h3>
 * <ul>
 * <li><b>Log Injection</b>: remove caracteres de controle (\r, \n, \t),
 * sequências ANSI-escape e trunca a saída para impedir
 * que dados do usuário poluam ou forjem linhas de log.</li>
 * <li><b>SQL Injection</b>: detecta padrões comuns de ataque SQL
 * (UNION, DROP, xp_cmdshell, comentários inline, tautologias)
 * para bloqueio preventivo antes de chegar ao banco.</li>
 * <li><b>Sanitização geral</b>: remove caracteres de controle ASCII
 * (0x00-0x1F, 0x7F) e limita o tamanho de strings para evitar
 * abusos de tamanho (buffer overflow lógico).</li>
 * </ul>
 *
 * <p>
 * Estas verificações são <b>complementares</b> ao uso de Prepared
 * Statements pelo JPA/Hibernate — representam defesa em profundidade.
 * </p>
 *
 * @since 1.1
 * @see SqlInjectionFilter
 * @see SensitiveDataMasker
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // utilitário estático
    }

    // ─── Log Injection ─────────────────────────────────────────────

    /**
     * Regex para sequências ANSI-escape (ex.: ESC[31m).
     * Atacantes injetam códigos ANSI para colorir ou ocultar texto em terminais.
     */
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\x1B\\[[0-9;]*[A-Za-z]");

    private static final int MAX_LOG_VALUE_LENGTH = 200;

    /**
     * Sanitiza um valor antes de incluí-lo em mensagens de log.
     *
     * <p>
     * <b>O que faz:</b>
     * </p>
     * <ol>
     * <li>Retorna "null" para valores nulos (evita NullPointerException)</li>
     * <li>Remove \r, \n e \t — impede injeção de novas linhas de log</li>
     * <li>Remove sequências ANSI-escape — impede manipulação visual de
     * terminais</li>
     * <li>Trunca em {@value #MAX_LOG_VALUE_LENGTH} caracteres — impede logs
     * gigantes</li>
     * </ol>
     *
     * <p>
     * <b>Cenário de ataque (Log Injection / Log Forging):</b>
     * </p>
     * 
     * <pre>
     * username = "admin\n2024-01-01 INFO Acesso PERMITIDO para admin"
     * // sem sanitização → cria linha falsa no arquivo de log
     * // com sanitização → "admin2024-01-01 INFO Acesso PERMITIDO para admin"
     * </pre>
     *
     * @param value valor a sanitizar (pode ser nulo)
     * @return valor seguro para inclusão em log
     */
    public static String sanitizeForLog(String value) {
        if (value == null) {
            return "null";
        }

        String safe = value
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "");

        safe = ANSI_ESCAPE.matcher(safe).replaceAll("");

        if (safe.length() > MAX_LOG_VALUE_LENGTH) {
            safe = safe.substring(0, MAX_LOG_VALUE_LENGTH) + "...[TRUNCATED]";
        }
        return safe;
    }

    // ─── SQL Injection ─────────────────────────────────────────────

    /**
     * Padrão que detecta palavras-chave e construções típicas de SQL Injection.
     *
     * <p>
     * <b>Detalhamento dos padrões:</b>
     * </p>
     * <table>
     * <tr>
     * <th>Padrão</th>
     * <th>Ataque que previne</th>
     * </tr>
     * <tr>
     * <td>UNION\s+(ALL\s+)?SELECT</td>
     * <td>Extração de dados de outras tabelas</td>
     * </tr>
     * <tr>
     * <td>DROP\s+TABLE</td>
     * <td>Destruição de tabelas</td>
     * </tr>
     * <tr>
     * <td>INSERT\s+INTO</td>
     * <td>Criação de registros maliciosos</td>
     * </tr>
     * <tr>
     * <td>DELETE\s+FROM</td>
     * <td>Remoção de dados</td>
     * </tr>
     * <tr>
     * <td>UPDATE\s+\w+\s+SET</td>
     * <td>Alteração de dados</td>
     * </tr>
     * <tr>
     * <td>xp_cmdshell</td>
     * <td>Execução de comandos no SO (SQL Server)</td>
     * </tr>
     * <tr>
     * <td>/\*.*\*‍/</td>
     * <td>Comentários inline para bypass de filtros</td>
     * </tr>
     * <tr>
     * <td>'\s*OR\s+'1'\s*=\s*'1</td>
     * <td>Tautologia clássica (bypass de login)</td>
     * </tr>
     * <tr>
     * <td>';|--</td>
     * <td>Terminação de query + comentário de linha</td>
     * </tr>
     * </table>
     *
     * <p>
     * Flag CASE_INSENSITIVE garante detecção independente de maiúsculas.
     * </p>
     */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)"
                    + "("
                    + "\\bUNION\\s+(ALL\\s+)?SELECT\\b" // UNION SELECT / UNION ALL SELECT
                    + "|\\bDROP\\s+TABLE\\b" // DROP TABLE
                    + "|\\bINSERT\\s+INTO\\b" // INSERT INTO
                    + "|\\bDELETE\\s+FROM\\b" // DELETE FROM
                    + "|\\bUPDATE\\s+\\w+\\s+SET\\b" // UPDATE <table> SET
                    + "|\\bxp_cmdshell\\b" // SQL Server command execution
                    + "|/\\*.*?\\*/" // comentários inline (/* ... */)
                    + "|'\\s*OR\\s+'1'\\s*=\\s*'1" // tautologia clássica
                    + "|';\\s*--" // terminação de query + comentário
                    + "|\\bEXEC(UTE)?\\s+" // EXEC/EXECUTE
                    + "|\\bSELECT\\s+.*\\bFROM\\s+information_schema\\b" // enum de schema
                    + "|\\bWAITFOR\\s+DELAY\\b" // time-based blind injection
                    + "|\\bBENCHMARK\\s*\\(" // MySQL time-based blind
                    + "|\\bSLEEP\\s*\\(" // MySQL SLEEP()
                    + ")");

    /**
     * Verifica se uma string contém padrões típicos de SQL Injection.
     *
     * <p>
     * <b>Cenário de ataque (SQL Injection clássica):</b>
     * </p>
     * 
     * <pre>
     *   // Tautologia em parâmetro de busca:
     *   GET /api/movies?title=' OR '1'='1
     *   // → Retorna TODOS os filmes, ignorando filtro
     *
     *   // UNION-based para extrair senhas:
     *   GET /api/movies?title=' UNION SELECT email,password FROM users --
     *   // → Senhas são retornadas junto com resultados de filmes
     * </pre>
     *
     * @param input texto a verificar
     * @return {@code true} se contiver padrão suspeito de SQL Injection
     */
    public static boolean containsSqlPattern(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    // ─── Sanitização geral ─────────────────────────────────────────

    /**
     * Sanitização genérica: remove caracteres de controle e limita tamanho.
     *
     * <p>
     * <b>Caracteres removidos:</b>
     * </p>
     * <ul>
     * <li>ASCII 0x00-0x1F (caracteres de controle: NUL, SOH, STX, etc.)</li>
     * <li>ASCII 0x7F (DEL)</li>
     * </ul>
     *
     * <p>
     * Preserva espaços (0x20), letras, números e pontuação normal.
     * </p>
     *
     * @param input     texto de entrada (pode ser nulo)
     * @param maxLength tamanho máximo permitido
     * @return texto sanitizado ou string vazia se nulo
     */
    public static String sanitize(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        // Remove caracteres de controle ASCII (0x00-0x1F) e DEL (0x7F)
        String cleaned = input.replaceAll("[\\x00-\\x1F\\x7F]", "");

        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }
}
