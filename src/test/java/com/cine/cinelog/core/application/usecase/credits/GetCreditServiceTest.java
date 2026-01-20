package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.Credit;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GetCreditServiceTest {

    @Test
    void execute_returnsCredit_whenFound() {
        CreditRepositoryPort repo = mock(CreditRepositoryPort.class);
        GetCreditService service = new GetCreditService(repo);

        Long id = 1L;
        Credit credit = mock(Credit.class);
        when(repo.findById(id)).thenReturn(Optional.of(credit));

        Credit result = service.execute(id);

        assertSame(credit, result);
        verify(repo).findById(id);
    }

    @Test
    void execute_throwsIllegalArgumentException_whenNotFound() {
        CreditRepositoryPort repo = mock(CreditRepositoryPort.class);
        GetCreditService service = new GetCreditService(repo);

        Long id = 42L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () -> service.execute(id));
        assertTrue(ex.getMessage().contains("Credit not found") || ex.getMessage().contains(id.toString()));
        verify(repo).findById(id);
    }
}
