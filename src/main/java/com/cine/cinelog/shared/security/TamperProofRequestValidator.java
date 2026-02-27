package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.security.IntegrityService.TamperDetectedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * A08:2025 — Validador anti-tampering para requests.
 *
 * <p>
 * Verifica que campos imutáveis em DTOs de request (userId, role)
 * não foram manipulados pelo cliente. Um atacante pode modificar
 * o JSON do request para trocar seu userId por outro, por exemplo.
 * </p>
 *
 * <h3>Cenário de ataque:</h3>
 * 
 * <pre>
 * POST /api/v1/reviews
 * {
 *   "userId": 999,     ← atacante trocou de 42 para 999
 *   "mediaId": 1,
 *   "rating": 10
 * }
 * </pre>
 *
 * <h3>Proteção:</h3>
 * 
 * <pre>
 * tamperProofValidator.validateUserOwnership(requestUserId);
 * // Se requestUserId != authenticated userId → TamperDetectedException
 * </pre>
 *
 * @since 1.3.0
 */
@Component
@Slf4j
public class TamperProofRequestValidator {

    /**
     * Valida que o userId no request corresponde ao usuário autenticado.
     *
     * @param requestUserId userId enviado no body/path do request
     * @throws TamperDetectedException se houver mismatch
     */
    public void validateUserOwnership(Long requestUserId) {
        Long authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId != null && !authenticatedUserId.equals(requestUserId)) {
            log.error("A08:2025 — TAMPER DETECTED: request userId={} != authenticated userId={}",
                    requestUserId, authenticatedUserId);
            throw new TamperDetectedException("Request", requestUserId.toString());
        }
    }

    /**
     * Valida que o email no request corresponde ao usuário autenticado.
     *
     * @param requestEmail email enviado no body do request
     * @throws TamperDetectedException se houver mismatch
     */
    public void validateEmailOwnership(String requestEmail) {
        String authenticatedEmail = getAuthenticatedEmail();
        if (authenticatedEmail != null && !authenticatedEmail.equalsIgnoreCase(requestEmail)) {
            log.error("A08:2025 — TAMPER DETECTED: request email não corresponde ao autenticado");
            throw new TamperDetectedException("Request", "email");
        }
    }

    /**
     * Valida que um campo role/admin não foi manipulado no request.
     * Respostas que contenham "role" só devem ser alteráveis por ADMIN.
     *
     * @param requestRole role enviada no body
     * @throws TamperDetectedException se usuário não-admin tentar definir role
     */
    public void validateRoleNotEscalated(String requestRole) {
        if (requestRole == null) {
            return; // campo não fornecido, ok
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return;
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !"USER".equalsIgnoreCase(requestRole)) {
            log.error("A08:2025 — TAMPER DETECTED: não-admin tentou definir role='{}'", requestRole);
            throw new TamperDetectedException("Request", "role");
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CinelogUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CinelogUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
