package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.enums.Role;
import com.cine.cinelog.core.domain.model.Credit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCreditServiceTest {

    @Mock
    private CreditRepositoryPort repo;

    @InjectMocks
    private UpdateCreditService service;

    @Test
    void shouldUpdateExistingCredit() {
        Long id = 1L;

        Credit existing = new Credit();
        existing.setRole(Role.ACTOR);
        existing.setPersonId(10L);
        existing.setMediaId(100L);

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        Credit update = new Credit();
        update.setRole(Role.DIRECTOR);
        update.setPersonId(20L);
        update.setMediaId(200L);

        Credit result = service.execute(id, update);

        assertSame(existing, result);
        assertEquals(Role.DIRECTOR, result.getRole());
        assertEquals(20L, result.getPersonId());
        assertEquals(200L, result.getMediaId());

        verify(repo).findById(id);
        verify(repo).save(existing);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowWhenCreditNotFound() {
        Long id = 2L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        Credit update = new Credit();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id, update));
        assertTrue(ex.getMessage().contains("Credit not found"));
        assertTrue(ex.getMessage().contains(String.valueOf(id)));

        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }
}