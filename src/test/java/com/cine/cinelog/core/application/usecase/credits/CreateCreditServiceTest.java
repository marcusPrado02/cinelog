package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateCreditServiceTest {

    @Test
    void should_create_credit_and_return_saved() {
        // Arrange
        var repo = mock(CreditRepositoryPort.class);
        var usecase = new CreateCreditService(repo);

        var inputCredit = mock(Credit.class);
        var savedCredit = mock(Credit.class);

        when(repo.save(inputCredit)).thenReturn(savedCredit);

        // Act
        var result = usecase.execute(inputCredit);

        // Assert
        assertThat(result).isSameAs(savedCredit);
        verify(repo).save(inputCredit);
    }

    @Test
    void should_call_repo_save_once_with_same_instance() {
        // Arrange
        var repo = mock(CreditRepositoryPort.class);
        var usecase = new CreateCreditService(repo);

        var credit = mock(Credit.class);
        when(repo.save(credit)).thenReturn(credit);

        // Act
        usecase.execute(credit);

        // Assert
        verify(repo, times(1)).save(credit);
    }
}