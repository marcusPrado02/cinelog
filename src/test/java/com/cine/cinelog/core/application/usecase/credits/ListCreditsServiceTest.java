package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListCreditsServiceTest {

    @Mock
    private CreditRepositoryPort repo;

    @InjectMocks
    private ListCreditsService service;

    @Test
    void execute_shouldReturnAllCreditsFromRepository() {
        Credit c1 = mock(Credit.class);
        Credit c2 = mock(Credit.class);
        List<Credit> expected = Arrays.asList(c1, c2);

        when(repo.findAll()).thenReturn(expected);

        List<Credit> result = service.execute();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expected, result);
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_shouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(repo.findAll()).thenReturn(Collections.emptyList());

        List<Credit> result = service.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }
}