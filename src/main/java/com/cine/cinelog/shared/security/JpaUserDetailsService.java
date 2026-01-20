package com.cine.cinelog.shared.security;

import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Serviço responsável por operação de JpaUserDetails.
 * Implementa o caso de uso de operação aplicando regras de negócio e políticas de domínio.
 * 
 * <p>Este serviço coordena as operações necessárias e garante
 * a consistência dos dados através de validações e políticas.</p>
 * 
 * @since 1.0
 * @see JpaUserDetailsService
 */
@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserJpaRepository userRepository;

    public JpaUserDetailsService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        return new CinelogUserDetails(user);
    }
}
