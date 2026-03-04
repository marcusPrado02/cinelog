package com.cine.cinelog.shared.security;

import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link CinelogUserDetails}.
 *
 * <p>
 * Garante que o mapeamento de roles para {@link GrantedAuthority} segue
 * a convenção Spring Security ({@code ROLE_<ROLE>}) — requisito do ADR-012.
 *
 * @see CinelogUserDetails
 * @see <a href="docs/adr/ADR-012-authorization-model.md">ADR-012</a>
 */
class CinelogUserDetailsTest {

    private UserEntity mockUser(String email, String passwordHash, String role, boolean enabled) {
        UserEntity u = mock(UserEntity.class);
        when(u.getEmail()).thenReturn(email);
        when(u.getPasswordHash()).thenReturn(passwordHash);
        when(u.getRole()).thenReturn(role);
        when(u.isEnabled()).thenReturn(enabled);
        return u;
    }

    // ─── getAuthorities ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAuthorities: role USER deve gerar ROLE_USER")
    void getAuthorities_userRole_shouldReturnRoleUser() {
        UserEntity entity = mockUser("user@cinelog.com", "hash", "USER", true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertThat(authorities)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("getAuthorities: role ADMIN deve gerar ROLE_ADMIN — ADR-012")
    void getAuthorities_adminRole_shouldReturnRoleAdmin() {
        UserEntity entity = mockUser("admin@cinelog.com", "hash", "ADMIN", true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertThat(authorities)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("getAuthorities: role OPS deve gerar ROLE_OPS")
    void getAuthorities_opsRole_shouldReturnRoleOps() {
        UserEntity entity = mockUser("ops@cinelog.com", "hash", "OPS", true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OPS");
    }

    @Test
    @DisplayName("getAuthorities: role nula deve usar default USER — STRIDE E1 (proteção contra escalada)")
    void getAuthorities_nullRole_shouldDefaultToRoleUser() {
        UserEntity entity = mockUser("x@cinelog.com", "hash", null, true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    // ─── getUsername / getPassword ────────────────────────────────────────────

    @Test
    @DisplayName("getUsername: deve retornar o e-mail da entidade")
    void getUsername_shouldReturnEmail() {
        UserEntity entity = mockUser("alice@cinelog.com", "someHash", "USER", true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        assertThat(details.getUsername()).isEqualTo("alice@cinelog.com");
    }

    @Test
    @DisplayName("getPassword: deve retornar o hash da senha da entidade")
    void getPassword_shouldReturnPasswordHash() {
        UserEntity entity = mockUser("alice@cinelog.com", "$2a$12$hashed", "USER", true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        assertThat(details.getPassword()).isEqualTo("$2a$12$hashed");
    }

    // ─── isEnabled / account flags ───────────────────────────────────────────

    @Test
    @DisplayName("isEnabled: deve refletir o campo enabled=true da entidade")
    void isEnabled_whenEntityEnabled_shouldReturnTrue() {
        UserEntity entity = mockUser("alice@cinelog.com", "hash", "USER", true);
        assertThat(new CinelogUserDetails(entity).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("isEnabled: deve refletir o campo enabled=false da entidade")
    void isEnabled_whenEntityDisabled_shouldReturnFalse() {
        UserEntity entity = mockUser("banned@cinelog.com", "hash", "USER", false);
        assertThat(new CinelogUserDetails(entity).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("accountNonExpired / NonLocked / credentialsNonExpired devem retornar true")
    void accountFlags_shouldAlwaysReturnTrue() {
        UserEntity entity = mockUser("alice@cinelog.com", "hash", "USER", true);
        CinelogUserDetails details = new CinelogUserDetails(entity);

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }
}
