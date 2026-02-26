package com.cine.cinelog.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Serviço de proteção contra enumeração de recursos (A04 — OWASP).
 *
 * <h3>O que é enumeração de usuários?</h3>
 * <p>
 * Acontece quando o atacante descobre <b>quais usuários existem</b> no sistema
 * baseando-se em diferenças nas respostas da API.
 * </p>
 *
 * <p>
 * <b>Cenário sem proteção:</b>
 * </p>
 * <table>
 * <tr>
 * <th>Cenário</th>
 * <th>Resposta insegura</th>
 * <th>O que o atacante descobre</th>
 * </tr>
 * <tr>
 * <td>Login com email inexistente</td>
 * <td>"Usuário não encontrado"</td>
 * <td>O email <b>não</b> existe</td>
 * </tr>
 * <tr>
 * <td>Login com email existente + senha errada</td>
 * <td>"Senha incorreta"</td>
 * <td>O email <b>existe</b></td>
 * </tr>
 * </table>
 *
 * <p>
 * Com essas informações, o atacante compila uma lista de emails válidos e faz
 * <b>credential stuffing</b> (testa senhas vazadas de outros serviços —
 * 65% dos usuários reutilizam senhas).
 * </p>
 *
 * <h3>Como prevenimos?</h3>
 * <ol>
 * <li><b>Mensagens genéricas:</b> sempre "Credenciais inválidas",
 * nunca "Usuário não encontrado" vs "Senha incorreta".</li>
 * <li><b>Timing noise:</b> delay aleatório para que o atacante não diferencie
 * "email não existe" (resposta rápida) de "email existe" (BCrypt mais
 * lento).</li>
 * <li><b>Erro genérico no registro:</b> "Se este email estiver disponível..."
 * em vez de "Email já cadastrado".</li>
 * </ol>
 *
 * <h3>O que é timing attack?</h3>
 * <p>
 * Quando o atacante mede o <b>tempo de resposta</b> para inferir informações.
 * Se login com email inexistente retorna em 5ms (sem hash) mas email existente
 * retorna em 300ms (fez BCrypt), o atacante sabe quais emails existem — mesmo
 * que a mensagem de erro seja idêntica.
 * </p>
 *
 * @since 1.1
 * @see RateLimitFilter
 */
@Service
@Slf4j
public class AntiEnumerationService {

    /** Mensagem genérica para login — NUNCA diferenciar email/senha. */
    public static final String GENERIC_AUTH_ERROR = "Credenciais inválidas.";

    /**
     * Mensagem genérica para registro/confirmação.
     * Não revela se o email já existe ou não.
     */
    public static final String GENERIC_REGISTRATION_MESSAGE = "Se este email estiver disponível, você receberá um link de confirmação.";

    /** Mensagem genérica para recuperação de senha. */
    public static final String GENERIC_RESET_MESSAGE = "Se este email estiver cadastrado, você receberá instruções de redefinição.";

    private final SecureRandom random = new SecureRandom();

    /**
     * Adiciona delay aleatório para normalizar o tempo de resposta.
     *
     * <p>
     * <b>Por que entre 100ms e 300ms?</b>
     * </p>
     * <ul>
     * <li>BCrypt com fator 12 leva ~200-400ms</li>
     * <li>O delay faz com que requests sem BCrypt (email inexistente)
     * tenham latência similar às com BCrypt (email existente)</li>
     * <li>A variação aleatória impede cálculo de média confiável</li>
     * </ul>
     *
     * <p>
     * <b>Por que {@link SecureRandom}?</b> {@link java.util.Random} usa algoritmo
     * determinístico — se o atacante descobrir o seed, pode prever os delays.
     * {@link SecureRandom} usa fonte de entropia do SO (/dev/urandom).
     * </p>
     */
    public void addTimingNoise() {
        try {
            int delayMs = 100 + random.nextInt(200); // 100-300ms
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Log seguro de tentativa de autenticação.
     * Não diferencia se o email existe ou não no nível do log.
     *
     * @param email   email informado (não logado em tentativas falhas)
     * @param success se a autenticação foi bem-sucedida
     */
    public void logAuthAttempt(String email, boolean success) {
        if (success) {
            log.info("Autenticação bem-sucedida");
        } else {
            // NÃO loga o email em tentativas falhas — evita enumeração via logs
            log.warn("Tentativa de autenticação falhou");
        }
    }
}
