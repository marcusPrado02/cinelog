package com.cine.cinelog.shared.config;

import com.cine.cinelog.shared.security.JwtAuthenticationFilter;
import com.cine.cinelog.shared.security.JwtTokenService;
import com.cine.cinelog.shared.security.RateLimitFilter;
import com.cine.cinelog.shared.security.SqlInjectionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Classe de configuração Spring para gerenciamento de security.
 *
 * <p>
 * Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.
 * </p>
 *
 * @since 1.0
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // A02: BCrypt com fator de trabalho 12 (mínimo recomendado OWASP)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenService, userDetailsService);
    }

    @Bean
    public SqlInjectionFilter sqlInjectionFilter() {
        return new SqlInjectionFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authProvider,
            JwtAuthenticationFilter jwtFilter,
            SqlInjectionFilter sqlInjectionFilter,
            RateLimitFilter rateLimitFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authProvider)
                // A02: Security headers para prevenir clickjacking, MIME sniffing, XSS
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(cto -> {
                        })
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/auth/**")
                        .permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info")
                        .permitAll()
                        // Endpoints ADMIN
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPS")

                        // Endpoints gerais (usuário logado)
                        .anyRequest().authenticated())
                // A04: Rate limit primeiro (bloqueia DoS antes de qualquer processamento)
                .addFilterBefore(rateLimitFilter, SqlInjectionFilter.class)
                // A03: Detecção de SQL injection antes de autenticação
                .addFilterBefore(sqlInjectionFilter, JwtAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
