package com.cine.cinelog.features.users.mapper;

import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.web.dto.UserCreateRequest;
import com.cine.cinelog.features.users.web.dto.UserResponse;
import com.cine.cinelog.features.users.web.dto.UserUpdateRequest;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper para conversão entre diferentes representações de usuários.
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
    UserEntity toEntity(User user);

    /**
     * Converte `UserEntity` (persistência) para o modelo de domínio `User`.
     * 
     * @param entity
     * @return
     */
    User toDomain(UserEntity entity);

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
