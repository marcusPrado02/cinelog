package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListSeasonsServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    @Test
    void execute_returnsSeasonsList() {
        Season s1 = mock(Season.class);
        Season s2 = mock(Season.class);
        List<Season> expected = Arrays.asList(s1, s2);

        when(repo.findAll()).thenReturn(expected);

        ListSeasonsService service = new ListSeasonsService(repo);
        List<Season> actual = service.execute();

        assertSame(expected, actual, "execute should return the list provided by the repository");
        verify(repo, times(1)).findAll();
    }

    @Test
    void execute_returnsEmptyListWhenRepositoryIsEmpty() {
        List<Season> expected = Collections.emptyList();

        when(repo.findAll()).thenReturn(expected);

        ListSeasonsService service = new ListSeasonsService(repo);
        List<Season> actual = service.execute();

        assertSame(expected, actual, "execute should return the empty list provided by the repository");
        verify(repo, times(1)).findAll();
    }
}