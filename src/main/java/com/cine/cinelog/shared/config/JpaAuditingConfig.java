package com.cine.cinelog.shared.config;

import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.shared.security.AuthenticatedUser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
/**
 * Classe de configuração Spring para gerenciamento de jpaauditing.
 * 
 * <p>Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.</p>
 * 
 * @since 1.0
 */
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware(CurrentUserProvider currentUserProvider) {
        return () -> {
            return currentUserProvider.getCurrentUser()
                    .map(AuthenticatedUser::id);
        };
    }
}
