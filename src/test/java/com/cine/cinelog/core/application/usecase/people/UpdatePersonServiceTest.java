package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdatePersonServiceTest {

    @Test
    void shouldUpdatePersonSuccessfully() {
        PersonRepositoryPort repo = mock(PersonRepositoryPort.class);
        UpdatePersonService service = new UpdatePersonService(repo);

        Long id = 1L;
        Person existing = new Person();
        existing.setName("Old Name");
        existing.setBirthDate(LocalDate.of(1990, 1, 1));
        existing.setPlaceOfBirth("Old Place");

        Person update = new Person();
        update.setName("New Name");
        update.setBirthDate(LocalDate.of(1991, 2, 2));
        update.setPlaceOfBirth("New Place");

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        Person result = service.execute(id, update);

        assertSame(existing, result);
        assertEquals("New Name", existing.getName());
        assertEquals(LocalDate.of(1991, 2, 2), existing.getBirthDate());
        assertEquals("New Place", existing.getPlaceOfBirth());

        verify(repo).findById(id);
        verify(repo).save(existing);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowWhenPersonNotFound() {
        PersonRepositoryPort repo = mock(PersonRepositoryPort.class);
        UpdatePersonService service = new UpdatePersonService(repo);

        Long id = 2L;
        Person update = new Person();
        update.setName("Name");

        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id, update));
        assertTrue(ex.getMessage().contains("Person not found: " + id));

        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }
}