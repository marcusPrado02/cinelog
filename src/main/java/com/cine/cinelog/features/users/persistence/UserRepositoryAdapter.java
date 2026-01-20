package com.cine.cinelog.features.users.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de repositório para persistência de User.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 *
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre User e UserEntity.
 * </p>
 *
 * @since 1.0
 * @see UserRepositoryPort
 * @see UserEntity
 * @see User
 */
@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryAdapter.class);

    private final UserJpaRepository jpa;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(UserJpaRepository jpa, UserMapper userMapper) {
        this.jpa = jpa;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        log.debug("Iniciando save. Parâmetros: userId={}, email={}",
                user.getId() != null ? user.getId() : "novo", user.getEmail());
        try {
            // Garantir que campos de auditoria não sejam null para novos usuários
            if (user.getId() == null) {
                if (user.getCreatedAt() == null) {
                    user.setCreatedAt(java.time.LocalDateTime.now());
                }
                if (user.getUpdatedAt() == null) {
                    user.setUpdatedAt(java.time.LocalDateTime.now());
                }
                // createdBy e updatedBy podem ser null para registro público
                // Apenas garantir que version não seja null
                if (user.getVersion() == null) {
                    user.setVersion(0L);
                }
            }

            log.debug("Antes do mapping: user.name={}, user.email={}", user.getName(), user.getEmail());
            var entity = userMapper.toEntity(user);
            log.debug("Depois do mapping: entity.name={}, entity.email={}", entity.getName(), entity.getEmail());
            var saved = jpa.save(entity);
            User result = userMapper.toDomain(saved);
            log.debug("Finalizando save. Resultado: ID={}", result.getId());
            return result;
        } catch (Exception e) {
            log.error("Erro ao salvar usuário. userId={}, email={}, erro={}",
                    user.getId(), user.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("Iniciando findById. Parâmetros: {}", Map.of("id", id));
        try {
            Optional<User> result = jpa.findById(id).map(userMapper::toDomain);
            log.debug("Finalizando findById. Resultado: {}", result.isPresent() ? "encontrado" : "não encontrado");
            return result;
        } catch (Exception e) {
            log.error("Erro ao buscar usuário por ID. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public PageResult<User> findAll(PageQuery query) {
        log.debug("Iniciando findAll. Parâmetros: {}", Map.of("page", query.page(), "size", query.size()));
        try {
            Pageable pageable = PageRequest.of(
                    query.page(),
                    query.size(),
                    Sort.by(Sort.Direction.fromString(query.direction()), query.sort()));

            Page<UserEntity> page = jpa.findAll(pageable);
            PageResult<User> result = PageResultMapper.from(page, userMapper::toDomain);

            log.debug("Finalizando findAll. Resultado: {} usuários encontrados", result.content().size());
            return result;
        } catch (Exception e) {
            log.error("Erro ao listar usuários. Parâmetros: {}. Erro: {}",
                    Map.of("page", query.page(), "size", query.size()), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteById(Long id) {
        log.debug("Iniciando deleteById. Parâmetros: {}", Map.of("id", id));
        try {
            jpa.deleteById(id);
            log.debug("Finalizando deleteById. ID: {}", id);
        } catch (Exception e) {
            log.error("Erro ao remover usuário por ID. Parâmetros: {}. Erro: {}", Map.of("id", id), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Measured("cinelog.repository.user.existsByEmail")
    @AlertIfSlow(thresholdMs = 500)
    public boolean existsByEmail(String email) {
        log.debug("Iniciando existsByEmail. Parâmetros: {}", Map.of("email", email));
        try {
            boolean exists = jpa.existsByEmail(email);
            log.debug("Finalizando existsByEmail. Resultado: {}", exists);
            return exists;
        } catch (Exception e) {
            log.error("Erro ao verificar existência de email. Parâmetros: {}. Erro: {}", Map.of("email", email),
                    e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        log.debug("Iniciando existsByEmailAndIdNot. Parâmetros: {}", Map.of("email", email, "id", id));
        try {
            boolean exists = jpa.existsByEmailAndIdNot(email, id);
            log.debug("Finalizando existsByEmailAndIdNot. Resultado: {}", exists);
            return exists;
        } catch (Exception e) {
            log.error("Erro ao verificar existência de email (excluindo ID). Parâmetros: {}. Erro: {}",
                    Map.of("email", email, "id", id), e.getMessage(), e);
            throw e;
        }
    }
}
