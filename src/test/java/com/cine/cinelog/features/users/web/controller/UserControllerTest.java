package com.cine.cinelog.features.users.web.controller;

import com.cine.cinelog.core.application.ports.in.user.CreateUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.DeleteUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.GetUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.ListUsersUseCase;
import com.cine.cinelog.core.application.ports.in.user.UpdateUserUseCase;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.web.dto.UserCreateRequest;
import com.cine.cinelog.features.users.web.dto.UserResponse;
import com.cine.cinelog.features.users.web.dto.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
    private UserMapper mapper;

    @InjectMocks
    private UserController controller;

    @Test
    void create_shouldReturnCreatedResponseWithLocationAndBody() {
        UserCreateRequest req = mock(UserCreateRequest.class);
        User domainFromReq = mock(User.class);
        User created = mock(User.class);
        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(createUC.execute(domainFromReq)).thenReturn(created);
        when(created.getId()).thenReturn(42L);

        UserResponse expectedResponse = mock(UserResponse.class);
        when(mapper.toResponse(created)).thenReturn(expectedResponse);

        ResponseEntity<UserResponse> resp = controller.create(req);

        assertEquals(201, resp.getStatusCodeValue());
        assertEquals(URI.create("/api/users/42"), resp.getHeaders().getLocation());
        assertSame(expectedResponse, resp.getBody());
    }

    @Test
    void update_shouldReturnOkWithUpdatedBody() {
        Long id = 7L;
        UserUpdateRequest req = mock(UserUpdateRequest.class);
        User domainFromReq = mock(User.class);
        User updated = mock(User.class);

        when(mapper.toDomain(req)).thenReturn(domainFromReq);
        when(updateUC.execute(id, domainFromReq)).thenReturn(updated);

        UserResponse expectedResponse = mock(UserResponse.class);
        when(mapper.toResponse(updated)).thenReturn(expectedResponse);

        ResponseEntity<UserResponse> resp = controller.update(id, req);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(expectedResponse, resp.getBody());
    }

    @Test
    void getById_shouldReturnOkWithBody() {
        Long id = 13L;
        User found = mock(User.class);
        when(getUC.execute(id)).thenReturn(found);

        UserResponse expectedResponse = mock(UserResponse.class);
        when(mapper.toResponse(found)).thenReturn(expectedResponse);

        ResponseEntity<UserResponse> resp = controller.getById(id);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(expectedResponse, resp.getBody());
    }

    @Test
    void list_shouldReturnAllUsersMappedToResponses() {
        User u1 = mock(User.class);
        User u2 = mock(User.class);
        List<User> users = Arrays.asList(u1, u2);
        when(listUC.execute()).thenReturn(users);

        UserResponse r1 = mock(UserResponse.class);
        UserResponse r2 = mock(UserResponse.class);
        when(mapper.toResponse(u1)).thenReturn(r1);
        when(mapper.toResponse(u2)).thenReturn(r2);

        ResponseEntity<List<UserResponse>> resp = controller.list();

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(2, resp.getBody().size());
        assertEquals(r1, resp.getBody().get(0));
        assertEquals(r2, resp.getBody().get(1));
    }

    @Test
    void delete_shouldCallUseCaseAndReturnNoContent() {
        Long id = 99L;

        ResponseEntity<Void> resp = controller.delete(id);

        verify(deleteUC).execute(id);
        assertEquals(204, resp.getStatusCodeValue());
        assertNull(resp.getBody());
    }
}