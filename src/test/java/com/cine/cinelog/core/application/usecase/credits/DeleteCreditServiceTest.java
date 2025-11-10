package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DeleteCreditServiceTest {

    @Test
    void should_delete_credit_by_id() {
        // Arrange
        var repo = mock(CreditRepositoryPort.class);
        var service = new DeleteCreditService(repo);
        Long id = 1L;

        // Act
        service.execute(id);

        // Assert
        verify(repo).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void should_propagate_exception_from_repository() {
        // Arrange
        var repo = mock(CreditRepositoryPort.class);
        var service = new DeleteCreditService(repo);
        Long id = 2L;
        doThrow(new RuntimeException("db error")).when(repo).deleteById(id);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.execute(id));
        verify(repo).deleteById(id);
    }
}