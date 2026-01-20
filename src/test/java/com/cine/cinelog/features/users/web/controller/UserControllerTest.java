package com.cine.cinelog.features.users.web.controller;

import com.cine.cinelog.core.application.ports.in.user.*;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.model.UserStats;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.web.dto.UserCreateRequest;
import com.cine.cinelog.features.users.web.dto.UserResponse;
import com.cine.cinelog.features.users.web.dto.UserUpdateRequest;
import com.cine.cinelog.features.users.web.dto.UserStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private CreateUserUseCase createUC;
    @Mock
    private UpdateUserUseCase updateUC;
    @Mock
    private GetUserUseCase getUC;
    @Mock
    private ListUsersUseCase listUC;
    @Mock
    private DeleteUserUseCase deleteUC;
    @Mock
    private GetMyStatsUseCase getMyStatsUseCase;
    @Mock
    private UserMapper mapper;

    @Mock
    private com.cine.cinelog.shared.observability.metrics.BusinessMetricsService metricsService;

    @InjectMocks
    private UserController controller;

    @Test
    void create_shouldReturnCreatedResponseWithLocationAndBody() {
        UserCreateRequest req = mock(UserCreateRequest.class);
        when(req.name()).thenReturn("Test User");
        when(req.email()).thenReturn("test@example.com");

        User domainFromReq = mock(User.class);
        User created = mock(User.class);
        UserResponse resp = mock(UserResponse.class);

        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(createUC.execute(domainFromReq)).thenReturn(created);
        when(created.getId()).thenReturn(42L);
        when(mapper.toResponse(created)).thenReturn(resp);

        ResponseEntity<UserResponse> result = controller.create(req);

        assertEquals(201, result.getStatusCodeValue());
        assertEquals(URI.create("/api/users/42"), result.getHeaders().getLocation());
        assertSame(resp, result.getBody());

        verify(mapper).toDomain(req);
        verify(createUC).execute(domainFromReq);
        verify(mapper).toResponse(created);
    }

    @Test
    void update_shouldReturnOkWithMappedResponse() {
        Long id = 7L;
        UserUpdateRequest req = mock(UserUpdateRequest.class);
        when(req.name()).thenReturn("Updated Name");

        User domainFromReq = mock(User.class);
        User updated = mock(User.class);
        UserResponse resp = mock(UserResponse.class);

        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(updateUC.execute(id, domainFromReq)).thenReturn(updated);
        when(mapper.toResponse(updated)).thenReturn(resp);

        ResponseEntity<UserResponse> result = controller.update(id, req);

        assertEquals(200, result.getStatusCodeValue());
        assertSame(resp, result.getBody());

        verify(mapper).toDomain(req);
        verify(updateUC).execute(id, domainFromReq);
        verify(mapper).toResponse(updated);
    }

    @Test
    void getById_shouldReturnOkWithMappedResponse() {
        Long id = 13L;
        User user = mock(User.class);
        UserResponse resp = mock(UserResponse.class);

        when(getUC.execute(id)).thenReturn(user);
        when(mapper.toResponse(user)).thenReturn(resp);

        ResponseEntity<UserResponse> result = controller.getById(id);

        assertEquals(200, result.getStatusCodeValue());
        assertSame(resp, result.getBody());

        verify(getUC).execute(id);
        verify(mapper).toResponse(user);
    }

    @Test
    void delete_shouldCallUseCaseAndReturnNoContent() {
        Long id = 99L;

        ResponseEntity<Void> result = controller.delete(id);

        assertEquals(204, result.getStatusCodeValue());
        verify(deleteUC).execute(id);
    }

    @Test
    void getMyStats_shouldReturnMappedStatsResponse() {
        UserStats stats = mock(UserStats.class);

        when(getMyStatsUseCase.execute()).thenReturn(stats);
        when(stats.getTotalEntries()).thenReturn(10L);
        when(stats.getTotalRated()).thenReturn(8L);
        when(stats.getAverageRating()).thenReturn(4.25);
        when(stats.getFirstWatchDate()).thenReturn(LocalDate.parse("2020-01-01"));
        when(stats.getLastWatchDate()).thenReturn(LocalDate.parse("2021-12-31"));

        UserStatsResponse resp = controller.getMyStats();

        assertNotNull(resp);
        assertEquals(10L, resp.totalEntries());
        assertEquals(8L, resp.totalRated());
        assertEquals(4.25, resp.averageRating());
        assertEquals(LocalDate.parse("2020-01-01"), resp.firstWatchDate());
        assertEquals(LocalDate.parse("2021-12-31"), resp.lastWatchDate());

        verify(getMyStatsUseCase).execute();
    }
}