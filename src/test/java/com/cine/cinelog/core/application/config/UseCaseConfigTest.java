package com.cine.cinelog.core.application.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.cine.cinelog.core.application.ports.in.credits.CreateCreditUseCase;
import com.cine.cinelog.core.application.ports.in.credits.DeleteCreditUseCase;
import com.cine.cinelog.core.application.ports.in.credits.GetCreditUseCase;
import com.cine.cinelog.core.application.ports.in.credits.ListCreditsUseCase;
import com.cine.cinelog.core.application.ports.in.credits.UpdateCreditUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.CreateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.DeleteEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.GetEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.ListEpisodesUseCase;
import com.cine.cinelog.core.application.ports.in.episodes.UpdateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.in.genre.CreateGenreUseCase;
import com.cine.cinelog.core.application.ports.in.genre.DeleteGenreUseCase;
import com.cine.cinelog.core.application.ports.in.genre.GetGenreUseCase;
import com.cine.cinelog.core.application.ports.in.genre.ListGenresUseCase;
import com.cine.cinelog.core.application.ports.in.genre.UpdateGenreUseCase;
import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.DeleteMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.GetMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.ListMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.UpdateMediaUseCase;
import com.cine.cinelog.core.application.ports.in.person.CreatePersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.DeletePersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.GetPersonUseCase;
import com.cine.cinelog.core.application.ports.in.person.ListPeopleUseCase;
import com.cine.cinelog.core.application.ports.in.person.UpdatePersonUseCase;
import com.cine.cinelog.core.application.ports.in.season.CreateSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.season.DeleteSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.season.GetSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.season.ListSeasonsUseCase;
import com.cine.cinelog.core.application.ports.in.season.UpdateSeasonUseCase;
import com.cine.cinelog.core.application.ports.in.user.CreateUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.DeleteUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.GetUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.ListUsersUseCase;
import com.cine.cinelog.core.application.ports.in.user.UpdateUserUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.CreateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.DeleteWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.GetWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.ListWatchEntriesUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.UpdateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.policy.MediaPolicy;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultMediaPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultRatingPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultWatchEntryPolicy;

class UseCaseConfigTest {

    @Test
    void policyBeans_shouldReturnDefaultImplementations() {
        UseCaseConfig cfg = new UseCaseConfig();

        MediaPolicy mediaPolicy = cfg.mediaPolicy();
        RatingPolicy ratingPolicy = cfg.ratingPolicy();
        WatchEntryPolicy watchEntryPolicy = cfg.watchEntryPolicy();

        assertNotNull(mediaPolicy);
        assertTrue(mediaPolicy instanceof DefaultMediaPolicy);

        assertNotNull(ratingPolicy);
        assertTrue(ratingPolicy instanceof DefaultRatingPolicy);

        assertNotNull(watchEntryPolicy);
        assertTrue(watchEntryPolicy instanceof DefaultWatchEntryPolicy);
    }

    @Test
    void mediaUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        MediaRepositoryPort repo = Mockito.mock(MediaRepositoryPort.class);

        CreateMediaUseCase create = cfg.createMediaUseCase(repo, cfg.mediaPolicy());
        UpdateMediaUseCase update = cfg.updateMediaUseCase(repo, cfg.mediaPolicy());
        GetMediaUseCase get = cfg.getMediaUseCase(repo);
        ListMediaUseCase list = cfg.listMediaUseCase(repo);
        DeleteMediaUseCase delete = cfg.deleteMediaUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateMediaUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateMediaUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetMediaUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListMediaUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteMediaUseCase);
    }

    @Test
    void userUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        UserRepositoryPort repo = Mockito.mock(UserRepositoryPort.class);

        CreateUserUseCase create = cfg.createUserUseCase(repo);
        UpdateUserUseCase update = cfg.updateUserUseCase(repo);
        GetUserUseCase get = cfg.getUserUseCase(repo);
        ListUsersUseCase list = cfg.listUsersUseCase(repo);
        DeleteUserUseCase delete = cfg.deleteUserUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateUserUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateUserUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetUserUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListUsersUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteUserUseCase);
    }

    @Test
    void genreUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        GenreRepositoryPort repo = Mockito.mock(GenreRepositoryPort.class);

        CreateGenreUseCase create = cfg.createGenreUseCase(repo);
        UpdateGenreUseCase update = cfg.updateGenreUseCase(repo);
        GetGenreUseCase get = cfg.getGenreUseCase(repo);
        ListGenresUseCase list = cfg.listGenresUseCase(repo);
        DeleteGenreUseCase delete = cfg.deleteGenreUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateGenreUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateGenreUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetGenreUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListGenresUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteGenreUseCase);
    }

    @Test
    void personUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        PersonRepositoryPort repo = Mockito.mock(PersonRepositoryPort.class);

        CreatePersonUseCase create = cfg.createPersonUseCase(repo);
        UpdatePersonUseCase update = cfg.updatePersonUseCase(repo);
        GetPersonUseCase get = cfg.getPersonUseCase(repo);
        ListPeopleUseCase list = cfg.listPeopleUseCase(repo);
        DeletePersonUseCase delete = cfg.deletePersonUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreatePersonUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdatePersonUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetPersonUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListPeopleUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeletePersonUseCase);
    }

    @Test
    void creditUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        CreditRepositoryPort repo = Mockito.mock(CreditRepositoryPort.class);

        CreateCreditUseCase create = cfg.createCreditUseCase(repo);
        UpdateCreditUseCase update = cfg.updateCreditUseCase(repo);
        GetCreditUseCase get = cfg.getCreditUseCase(repo);
        ListCreditsUseCase list = cfg.listCreditsUseCase(repo);
        DeleteCreditUseCase delete = cfg.deleteCreditUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateCreditUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateCreditUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetCreditUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListCreditsUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteCreditUseCase);
    }

    @Test
    void seasonUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        SeasonRepositoryPort repo = Mockito.mock(SeasonRepositoryPort.class);

        CreateSeasonUseCase create = cfg.createSeasonUseCase(repo);
        UpdateSeasonUseCase update = cfg.updateSeasonUseCase(repo);
        GetSeasonUseCase get = cfg.getSeasonUseCase(repo);
        ListSeasonsUseCase list = cfg.listSeasonsUseCase(repo);
        DeleteSeasonUseCase delete = cfg.deleteSeasonUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateSeasonUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateSeasonUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetSeasonUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListSeasonsUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteSeasonUseCase);
    }

    @Test
    void episodeUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        EpisodeRepositoryPort repo = Mockito.mock(EpisodeRepositoryPort.class);

        CreateEpisodeUseCase create = cfg.createEpisodeUseCase(repo);
        UpdateEpisodeUseCase update = cfg.updateEpisodeUseCase(repo);
        GetEpisodeUseCase get = cfg.getEpisodeUseCase(repo);
        ListEpisodesUseCase list = cfg.listEpisodesUseCase(repo);
        DeleteEpisodeUseCase delete = cfg.deleteEpisodeUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateEpisodeUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateEpisodeUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetEpisodeUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListEpisodesUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteEpisodeUseCase);
    }

    @Test
    void watchEntryUseCases_shouldBeCreated() {
        UseCaseConfig cfg = new UseCaseConfig();
        WatchEntryRepositoryPort repo = Mockito.mock(WatchEntryRepositoryPort.class);

        CreateWatchEntryUseCase create = cfg.createWatchEntryUseCase(repo);
        UpdateWatchEntryUseCase update = cfg.updateWatchEntryUseCase(repo, cfg.watchEntryPolicy(), cfg.ratingPolicy());
        GetWatchEntryUseCase get = cfg.getWatchEntryUseCase(repo);
        ListWatchEntriesUseCase list = cfg.listWatchEntriesUseCase(repo);
        DeleteWatchEntryUseCase delete = cfg.deleteWatchEntryUseCase(repo);

        assertNotNull(create);
        assertTrue(create instanceof CreateWatchEntryUseCase);

        assertNotNull(update);
        assertTrue(update instanceof UpdateWatchEntryUseCase);

        assertNotNull(get);
        assertTrue(get instanceof GetWatchEntryUseCase);

        assertNotNull(list);
        assertTrue(list instanceof ListWatchEntriesUseCase);

        assertNotNull(delete);
        assertTrue(delete instanceof DeleteWatchEntryUseCase);
    }
}