package com.cine.cinelog.features.auth.web.dto;

/**
 * DTO de resposta contendo o token JWT após autenticação bem-sucedida.
 * 
 * <p>
 * Retorna:
 * <ul>
 * <li>accessToken: JWT que deve ser enviado no header Authorization das
 * próximas requisições</li>
 * <li>tokenType: tipo do token (sempre "Bearer")</li>
 * </ul>
 * 
 * <p>
 * Exemplo de uso no client:
 * 
 * <pre>
 * Authorization: Bearer {accessToken}
 * </pre>
 * 
 * @since 1.0
 */
public record LoginResponse(
        String accessToken,
        String tokenType) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
