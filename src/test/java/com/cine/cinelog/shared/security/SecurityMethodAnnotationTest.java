package com.cine.cinelog.shared.security;

import com.cine.cinelog.features.media.web.controller.AdminMediaController;
import com.cine.cinelog.features.users.web.controller.UserController;
import com.cine.cinelog.features.watchentry.web.controller.WatchEntryController;
import com.cine.cinelog.infrastructure.web.controllers.admin.DeadLetterAdminController;
import com.cine.cinelog.shared.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a presença e o conteúdo das anotações de segurança nos controllers.
 *
 * <p>
 * Verifica (via reflexão) que os endpoints críticos possuem
 * {@link PreAuthorize}
 * e {@link PostAuthorize} conforme definido no ADR-012.
 *
 * <p>
 * Estes testes capturam regressões onde uma anotação é removida acidentalmente.
 * Não requerem Spring ApplicationContext — executam em < 1 segundo.
 *
 * @see <a href="docs/adr/ADR-012-authorization-model.md">ADR-012 — Modelo de
 *      Autorização</a>
 */
class SecurityMethodAnnotationTest {

    // ─── @EnableMethodSecurity ────────────────────────────────────────────────

    @Test
    @DisplayName("SecurityConfig deve ter @EnableMethodSecurity habilitado")
    void securityConfig_shouldHaveEnableMethodSecurity() {
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class))
                .as("SecurityConfig deve ter @EnableMethodSecurity")
                .isTrue();
    }

    // ─── AdminMediaController — nível de classe ───────────────────────────────

    @Test
    @DisplayName("AdminMediaController: classe deve ter @PreAuthorize(hasRole ADMIN) — ADR-012")
    void adminMediaController_classShouldHavePreAuthorizeAdmin() {
        PreAuthorize annotation = AdminMediaController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation)
                .as("AdminMediaController deve ter @PreAuthorize na classe")
                .isNotNull();
        assertThat(annotation.value())
                .as("Deve exigir role ADMIN")
                .contains("ADMIN");
    }

    // ─── DeadLetterAdminController — nível de classe ─────────────────────────

    @Test
    @DisplayName("DeadLetterAdminController: classe deve ter @PreAuthorize(hasAnyRole ADMIN/OPS)")
    void deadLetterAdminController_classShouldHavePreAuthorizeAdminOrOps() {
        PreAuthorize annotation = DeadLetterAdminController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation)
                .as("DeadLetterAdminController deve ter @PreAuthorize na classe")
                .isNotNull();
        assertThat(annotation.value())
                .contains("ADMIN")
                .contains("OPS");
    }

    // ─── UserController — list() ──────────────────────────────────────────────

    @Test
    @DisplayName("UserController.list: deve ter @PreAuthorize(hasRole ADMIN) — ADR-012")
    void userController_listMethod_shouldHavePreAuthorizeAdmin() {
        Optional<Method> method = findMethodByName(UserController.class, "list");

        assertThat(method).as("Método list deve existir em UserController").isPresent();

        PreAuthorize annotation = method.get().getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("UserController.list deve ter @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("UserController.list deve exigir role ADMIN")
                .contains("ADMIN");
    }

    // ─── UserController — delete() ────────────────────────────────────────────

    @Test
    @DisplayName("UserController.delete: deve ter @PreAuthorize(hasRole ADMIN)")
    void userController_deleteMethod_shouldHavePreAuthorizeAdmin() {
        Optional<Method> method = findMethodByName(UserController.class, "delete");

        assertThat(method).isPresent();

        PreAuthorize annotation = method.get().getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("UserController.delete deve ter @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value()).contains("ADMIN");
    }

    // ─── UserController — getById() ───────────────────────────────────────────

    @Test
    @DisplayName("UserController.getById: deve ter @PostAuthorize para proteger data de outros usuários — STRIDE I3")
    void userController_getByIdMethod_shouldHavePostAuthorize() {
        Optional<Method> method = findMethodByName(UserController.class, "getById");

        assertThat(method).isPresent();

        PostAuthorize annotation = method.get().getAnnotation(PostAuthorize.class);
        assertThat(annotation)
                .as("UserController.getById deve ter @PostAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("Deve verificar ownership por email")
                .contains("authentication.name");
    }

    // ─── UserController — update() ────────────────────────────────────────────

    @Test
    @DisplayName("UserController.update: deve ter @PreAuthorize permitindo ADMIN ou owner — STRIDE E3")
    void userController_updateMethod_shouldHavePreAuthorizeWithAdminOrOwner() {
        Optional<Method> method = findMethodByName(UserController.class, "update");

        assertThat(method).isPresent();

        PreAuthorize annotation = method.get().getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("UserController.update deve ter @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("Deve permitir ADMIN ou owner")
                .contains("ADMIN");
    }

    // ─── WatchEntryController — list() ───────────────────────────────────────

    @Test
    @DisplayName("WatchEntryController.list: deve ter @PreAuthorize com verificação de userId — STRIDE E3")
    void watchEntryController_listMethod_shouldHavePreAuthorize() {
        Optional<Method> method = findMethodByName(WatchEntryController.class, "list");

        assertThat(method).isPresent();

        PreAuthorize annotation = method.get().getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("WatchEntryController.list deve ter @PreAuthorize")
                .isNotNull();
        assertThat(annotation.value())
                .as("Deve verificar role ADMIN ou userId do principal")
                .contains("ADMIN");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Busca um método por nome (ignora sobrecargas).
     * Funciona para nomes únicos nos controllers do CineLog.
     */
    private Optional<Method> findMethodByName(Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst();
    }
}
