package com.cine.cinelog.features.users.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import com.cine.cinelog.core.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
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

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @BeforeEach
    void setUp() {
        // adapter created by @InjectMocks
    }

    @Test
    void save_shouldConvertEntity_saveAndReturnDomain() {
        User domainInput = mock(User.class);
        UserEntity mappedEntity = mock(UserEntity.class);
        UserEntity savedEntity = mock(UserEntity.class);
        User mappedDomain = mock(User.class);

        when(userMapper.toEntity(domainInput)).thenReturn(mappedEntity);
        when(jpa.save(mappedEntity)).thenReturn(savedEntity);
        when(userMapper.toDomain(savedEntity)).thenReturn(mappedDomain);

        User result = adapter.save(domainInput);

        assertNotNull(result);
        assertSame(mappedDomain, result);
        verify(userMapper).toEntity(domainInput);
        verify(jpa).save(mappedEntity);
        verify(userMapper).toDomain(savedEntity);
    }

    @Test
    void findById_whenFound_shouldReturnMappedDomain() {
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
    void findById_whenNotFound_shouldReturnEmpty() {
        Long id = 2L;
        when(jpa.findById(id)).thenReturn(Optional.empty());

        Optional<User> result = adapter.findById(id);

        assertFalse(result.isPresent());
        verify(jpa).findById(id);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findAll_shouldBuildPageable_andMapEntitiesToDomain() {
        PageQuery query = mock(PageQuery.class);
        when(query.page()).thenReturn(1);
        when(query.size()).thenReturn(2);
        when(query.direction()).thenReturn("ASC");
        when(query.sort()).thenReturn("name");

        UserEntity e1 = mock(UserEntity.class);
        UserEntity e2 = mock(UserEntity.class);
        User d1 = mock(User.class);
        User d2 = mock(User.class);

        List<UserEntity> content = List.of(e1, e2);
        Pageable expectedPageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "name"));
        Page<UserEntity> page = new PageImpl<>(content, expectedPageable, content.size());

        when(jpa.findAll(any(Pageable.class))).thenReturn(page);
        when(userMapper.toDomain(e1)).thenReturn(d1);
        when(userMapper.toDomain(e2)).thenReturn(d2);

        PageResult<User> result = adapter.findAll(query);

        assertNotNull(result);
        // Verify pageable constructed and passed to jpa
        verify(jpa).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertEquals(1, captured.getPageNumber());
        assertEquals(2, captured.getPageSize());
        Sort.Order order = captured.getSort().getOrderFor("name");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());

        // Verify mapping invoked for each entity
        verify(userMapper).toDomain(e1);
        verify(userMapper).toDomain(e2);
    }

    @Test
    void deleteById_shouldDelegateToJpa() {
        Long id = 5L;
        doNothing().when(jpa).deleteById(id);

        adapter.deleteById(id);

        verify(jpa).deleteById(id);
    }
}