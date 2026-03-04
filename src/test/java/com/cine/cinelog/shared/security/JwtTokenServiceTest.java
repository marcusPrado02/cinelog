package com.cine.cinelog.shared.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para {@link JwtTokenService}.
 *
 * <p>
 * Cobre os fluxos de segurança definidos no Threat Model STRIDE:
 * <ul>
 * <li>S1 — Geração de token com assinatura HMAC-SHA256</li>
 * <li>S1 — Rejeição de token adulterado / assinatura inválida</li>
 * <li>S1 — Validação do comprimento mínimo do secret (≥ 32 chars)</li>
 * </ul>
 *
 * @see JwtTokenService
 * @see <a href="docs/security/THREAT-MODEL-STRIDE.md">STRIDE Threat Model</a>
 */
class JwtTokenServiceTest {

    private static final String VALID_SECRET = "cinelog-test-secret-key-minimum-32-characters!"; // 46 chars

    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        service = new JwtTokenService(VALID_SECRET, 3600);
    }

    // ─── generateToken ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken: deve retornar token JWT não-nulo para subject válido")
    void generateToken_shouldReturnNonNullJwt() {
        String token = service.generateToken("user@cinelog.com");

        assertThat(token).isNotNull().isNotBlank();
        // JWT padrão tem 3 partes separadas por '.'
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("generateToken: tokens distintos para subjects distintos")
    void generateToken_shouldProduceDifferentTokensForDifferentSubjects() {
        String token1 = service.generateToken("alice@cinelog.com");
        String token2 = service.generateToken("bob@cinelog.com");

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("generateToken: tokens distintos entre chamadas para o mesmo subject (iat difere)")
    void generateToken_sameSubjectConsecutiveCalls_shouldProduceDifferentTokensDueToIat()
            throws InterruptedException {
        String token1 = service.generateToken("alice@cinelog.com");
        Thread.sleep(1001); // garante iat diferente (precisão de segundos)
        String token2 = service.generateToken("alice@cinelog.com");

        assertThat(token1).isNotEqualTo(token2);
    }

    // ─── extractSubject ───────────────────────────────────────────────────────

    @Test
    @DisplayName("extractSubject: deve retornar o subject original do token gerado")
    void extractSubject_shouldReturnOriginalSubject() {
        String email = "security@cinelog.com";
        String token = service.generateToken(email);

        String subject = service.extractSubject(token);

        assertThat(subject).isEqualTo(email);
    }

    @Test
    @DisplayName("extractSubject: token expirado deve lançar JwtException — STRIDE S1")
    void extractSubject_withExpiredToken_shouldThrowJwtException() throws InterruptedException {
        JwtTokenService shortLivedService = new JwtTokenService(VALID_SECRET, 1);
        String token = shortLivedService.generateToken("user@cinelog.com");

        Thread.sleep(2000); // aguarda expiração

        assertThatThrownBy(() -> shortLivedService.extractSubject(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("extractSubject: token adulterado deve lançar JwtException — STRIDE S1/T2")
    void extractSubject_withTamperedPayload_shouldThrowJwtException() {
        String token = service.generateToken("user@cinelog.com");
        // Adultera o payload (parte 2 do JWT)
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + "YWRtaW5AY2luZWxvZy5jb20" + "." + parts[2];

        assertThatThrownBy(() -> service.extractSubject(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("extractSubject: token com assinatura inválida deve lançar JwtException — STRIDE S1")
    void extractSubject_withInvalidSignature_shouldThrowJwtException() {
        String token = service.generateToken("user@cinelog.com");
        // Altera o último char da assinatura
        String invalidToken = token.substring(0, token.length() - 1) + "X";

        assertThatThrownBy(() -> service.extractSubject(invalidToken))
                .isInstanceOf(Exception.class); // JwtException ou IllegalArgumentException
    }

    @Test
    @DisplayName("extractSubject: token malformado deve lançar exceção")
    void extractSubject_withMalformedToken_shouldThrowException() {
        assertThatThrownBy(() -> service.extractSubject("not.a.jwt"))
                .isInstanceOf(Exception.class);
    }

    // ─── Validação do secret ──────────────────────────────────────────────────

    @Test
    @DisplayName("construtor: secret menor que 32 chars deve lançar IllegalStateException — STRIDE S1")
    void constructor_withShortSecret_shouldThrowIllegalStateException() {
        assertThatThrownBy(() -> new JwtTokenService("short-secret", 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("construtor: secret nulo deve lançar IllegalStateException")
    void constructor_withNullSecret_shouldThrowIllegalStateException() {
        assertThatThrownBy(() -> new JwtTokenService(null, 3600))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("construtor: secret exatamente 32 chars deve ser aceito")
    void constructor_withExactly32CharSecret_shouldBeAccepted() {
        // 32 chars: "12345678901234567890123456789012"
        assertThatCode(() -> new JwtTokenService("12345678901234567890123456789012", 3600))
                .doesNotThrowAnyException();
    }
}
