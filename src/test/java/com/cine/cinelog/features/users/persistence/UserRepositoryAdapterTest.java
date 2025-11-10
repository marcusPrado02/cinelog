package com.cine.cinelog.features.users.persistence;

import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    @Mock
    private UserJpaRepository jpa;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    @Test
    void save_shouldMapEntitySaveAndReturnDomain() {
        User domainInput = mock(User.class);
        UserEntity mappedEntity = mock(UserEntity.class);
        UserEntity savedEntity = mock(UserEntity.class);
        User domainOutput = mock(User.class);

        when(userMapper.toEntity(domainInput)).thenReturn(mappedEntity);
        when(jpa.save(mappedEntity)).thenReturn(savedEntity);
        when(userMapper.toDomain(savedEntity)).thenReturn(domainOutput);

        User result = adapter.save(domainInput);

        assertSame(domainOutput, result);
        verify(userMapper).toEntity(domainInput);
        verify(jpa).save(mappedEntity);
        verify(userMapper).toDomain(savedEntity);
        verifyNoMoreInteractions(userMapper, jpa);
    }

    @Test
    void findById_shouldReturnMappedOptionalWhenPresent() {
        Long id = 1L;
        UserEntity entity = mock(UserEntity.class);
        User domain = mock(User.class);

        when(jpa.findById(id)).thenReturn(Optional.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(domain);

        Optional<User> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
        verify(jpa).findById(id);
        verify(userMapper).toDomain(entity);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        Long id = 2L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<User> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findAll_shouldReturnAllMapped() {
        UserEntity e1 = mock(UserEntity.class);
        UserEntity e2 = mock(UserEntity.class);
        User d1 = mock(User.class);
        User d2 = mock(User.class);

        when(jpa.findAll()).thenReturn(List.of(e1, e2));
        when(userMapper.toDomain(e1)).thenReturn(d1);
        when(userMapper.toDomain(e2)).thenReturn(d2);

        List<User> result = adapter.findAll();

        assertEquals(2, result.size());
        assertSame(d1, result.get(0));
        assertSame(d2, result.get(1));
        verify(jpa).findAll();
        verify(userMapper).toDomain(e1);
        verify(userMapper).toDomain(e2);
    }

    @Test
    void deleteById_shouldDelegateToJpa() {
        Long id = 5L;
        doNothing().when(jpa).deleteById(ArgumentMatchers.eq(id));

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
        verifyNoInteractions(userMapper);
    }
}