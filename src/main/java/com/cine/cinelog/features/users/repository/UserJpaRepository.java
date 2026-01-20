package com.cine.cinelog.features.users.repository;

import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.watchentry.persistence.projection.UserStatsProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repositório JPA para gerenciamento de usuários.
 *
 * <p>
 * Fornece operações de persistência para {@link UserEntity}, incluindo:
 * <ul>
 * <li>CRUD básico herdado de JpaRepository</li>
 * <li>Busca por email</li>
 * <li>Verificação de existência de email</li>
 * <li>Verificação de unicidade de email (para updates)</li>
 * </ul>
 *
 * @since 1.0
 * @see UserEntity
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    /**
     * Busca um usuário pelo email.
     *
     * @param email o email do usuário
     * @return Optional contendo o usuário se encontrado
     */
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findByEmail(String email);

    /**
     * Verifica se existe um usuário com o email especificado.
     *
     * @param email o email a verificar
     * @return true se o email já está em uso, false caso contrário
     */
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.email = :email")
    boolean existsByEmail(String email);

    /**
     * Verifica se existe um usuário com o email especificado, excluindo um ID
     * específico.
     * Útil para validação de unicidade durante updates.
     *
     * @param email o email a verificar
     * @param id    o ID do usuário a excluir da verificação
     * @return true se outro usuário já usa este email, false caso contrário
     */
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.email = :email AND u.id != :id")
    boolean existsByEmailAndIdNot(String email, Long id);
}
