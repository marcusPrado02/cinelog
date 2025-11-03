package com.cine.cinelog.features.people.mapper;

import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import com.cine.cinelog.features.people.web.dto.PersonCreateRequest;
import com.cine.cinelog.features.people.web.dto.PersonResponse;
import com.cine.cinelog.features.people.web.dto.PersonUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper MapStruct para conversão entre DTOs REST e o modelo de domínio.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PersonMapper {

    /**
     * Converte o DTO de criação para o modelo de domínio `Person`.
     * 
     * @param request
     * @return
     */
    Person toDomain(PersonCreateRequest request);

    /**
     * Converte o modelo de domínio `Person` para o DTO de resposta.
     * 
     * @param person
     * @return
     */
    PersonResponse toResponse(Person person);

    /**
     * Converte o modelo de domínio `Person` para o DTO de criação.
     * 
     * @param person
     * @return
     */
    PersonUpdateRequest toUpdateRequest(Person person);

    /**
     * Converte o DTO de atualização para o modelo de domínio `Person`.
     * 
     * @param request
     * @return
     */
    Person toDomain(PersonUpdateRequest request);

    /**
     * Converte o modelo de domínio `Person` para a entidade JPA.
     * 
     * @param person
     * @return
     */
    PersonEntity toEntity(Person person);

    /**
     * Converte `PersonEntity` (persistência) para o modelo de domínio `Person`.
     * 
     * @param personEntity
     * @return
     */
    Person toDomain(PersonEntity personEntity);
}
