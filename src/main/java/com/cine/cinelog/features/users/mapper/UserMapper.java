package com.cine.cinelog.features.users.mapper;

import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.web.dto.UserCreateRequest;
import com.cine.cinelog.features.users.web.dto.UserResponse;
import com.cine.cinelog.features.users.web.dto.UserUpdateRequest;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper responsável pela conversão entre User e seus DTOs/Entidades.
 *
 * <p>
 * Utiliza MapStruct para gerar implementações automatizadas das conversões
 * entre:
 * <ul>
 * <li>Modelo de domínio (User)</li>
 * <li>DTOs de requisição/resposta (UserCreateRequest, UserUpdateRequest,
 * UserResponse)</li>
 * <li>Entidade de persistência (UserEntity)</li>
 * </ul>
 *
 * <p>
 * A configuração unmappedTargetPolicy = IGNORE permite que campos não mapeados
 * sejam ignorados silenciosamente, facilitando conversões parciais.
 *
 * @since 1.0
 * @see User
 * @see UserEntity
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    /**
     * Converte o modelo de domínio `User` para o DTO de resposta.
     *
     * @param user
     * @return
     */
    UserResponse toResponse(UserEntity user);

    /**
     * Converte o DTO de criação para a entidade JPA `UserEntity`.
     *
     * @param request
     * @return
     */
    UserEntity toEntity(UserCreateRequest request);

    /**
     * Converte a entidade JPA `UserEntity` para o DTO de criação.
     *
     * @param user
     * @return
     */
    UserCreateRequest toCreateRequest(UserEntity user);

    /**
     * Converte o DTO de resposta para a entidade JPA `UserEntity`.
     *
     * @param response
     * @return
     */
    UserEntity toEntityFromResponse(UserResponse response);

    /**
     * Converte a entidade JPA `UserEntity` para o DTO de resposta.
     *
     * @param user
     * @return
     */
    UserResponse toResponseFromEntity(UserEntity user);

    /**
     * Converte o modelo de domínio `User` para a entidade JPA.
     *
     * @param user
     * @return
     */
    default UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setCreatedBy(user.getCreatedBy());
        entity.setUpdatedBy(user.getUpdatedBy());
        entity.setVersion(user.getVersion());

        return entity;
    }

    /**
     * Converte `UserEntity` (persistência) para o modelo de domínio `User`.
     *
     * @param entity
     * @return
     */
    default User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();
        user.setId(entity.getId());
        user.setName(entity.getName());
        user.setEmail(entity.getEmail());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setCreatedBy(entity.getCreatedBy());
        user.setUpdatedBy(entity.getUpdatedBy());
        user.setVersion(entity.getVersion());

        return user;
    }

    /**
     * Converte o DTO de criação para o modelo de domínio `User`.
     *
     * @param req
     * @return
     */
    User toDomain(UserCreateRequest userCreateRequest);

    /**
     * Converte o modelo de domínio `User` para o DTO de resposta.
     *
     * @param user
     * @return
     */
    UserResponse toResponse(User user);

    /**
     * Converte o DTO de atualização para o modelo de domínio `User`.
     *
     * @param req
     * @return
     */
    User toDomain(UserUpdateRequest userUpdateRequest);

    /**
     * Converte o modelo de domínio `User` para o DTO de atualização.
     *
     * @param user
     * @return
     */
    UserUpdateRequest toUpdateRequest(User user);

}
