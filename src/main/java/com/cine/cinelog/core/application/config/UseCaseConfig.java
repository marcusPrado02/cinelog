package com.cine.cinelog.core.application.config;

import org.hibernate.sql.Update;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
import com.cine.cinelog.core.application.usecase.credits.CreateCreditService;
import com.cine.cinelog.core.application.usecase.credits.DeleteCreditService;
import com.cine.cinelog.core.application.usecase.credits.GetCreditService;
import com.cine.cinelog.core.application.usecase.credits.ListCreditsService;
import com.cine.cinelog.core.application.usecase.credits.UpdateCreditService;
import com.cine.cinelog.core.application.usecase.episodes.CreateEpisodeService;
import com.cine.cinelog.core.application.usecase.episodes.DeleteEpisodeService;
import com.cine.cinelog.core.application.usecase.episodes.GetEpisodeService;
import com.cine.cinelog.core.application.usecase.episodes.ListEpisodesService;
import com.cine.cinelog.core.application.usecase.episodes.UpdateEpisodeService;
import com.cine.cinelog.core.application.usecase.genre.CreateGenreService;
import com.cine.cinelog.core.application.usecase.genre.DeleteGenreService;
import com.cine.cinelog.core.application.usecase.genre.GetGenreService;
import com.cine.cinelog.core.application.usecase.genre.ListGenresService;
import com.cine.cinelog.core.application.usecase.genre.UpdateGenreService;
import com.cine.cinelog.core.application.usecase.media.CreateMediaService;
import com.cine.cinelog.core.application.usecase.media.DeleteMediaService;
import com.cine.cinelog.core.application.usecase.media.GetMediaService;
import com.cine.cinelog.core.application.usecase.media.ListMediaService;
import com.cine.cinelog.core.application.usecase.media.UpdateMediaService;
import com.cine.cinelog.core.application.usecase.people.CreatePersonService;
import com.cine.cinelog.core.application.usecase.people.DeletePersonService;
import com.cine.cinelog.core.application.usecase.people.GetPersonService;
import com.cine.cinelog.core.application.usecase.people.ListPeopleService;
import com.cine.cinelog.core.application.usecase.people.UpdatePersonService;
import com.cine.cinelog.core.application.usecase.seasons.UpdateSeasonService;
import com.cine.cinelog.core.application.usecase.seasons.CreateSeasonService;
import com.cine.cinelog.core.application.usecase.seasons.DeleteSeasonService;
import com.cine.cinelog.core.application.usecase.seasons.GetSeasonService;
import com.cine.cinelog.core.application.usecase.seasons.ListSeasonsService;
import com.cine.cinelog.core.application.usecase.user.CreateUserService;
import com.cine.cinelog.core.application.usecase.user.DeleteUserService;
import com.cine.cinelog.core.application.usecase.user.GetUserService;
import com.cine.cinelog.core.application.usecase.user.ListUsersService;
import com.cine.cinelog.core.application.usecase.user.UpdateUserService;
import com.cine.cinelog.core.application.usecase.watchentry.GetWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.ListWatchEntriesService;
import com.cine.cinelog.core.application.usecase.watchentry.CreateWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.DeleteWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.UpdateWatchEntryService;
import com.cine.cinelog.core.domain.policy.MediaPolicy;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultMediaPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultRatingPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultWatchEntryPolicy;

/**
 * Configuração dos casos de uso da aplicação.
 * 
 * Cada método cria e expõe um bean correspondente a um caso de uso,
 * injetando a porta de repositório necessária.
 */
@Configuration
public class UseCaseConfig {
    // ===== Policies (beans) =====
    @Bean
    public MediaPolicy mediaPolicy() {
        return new DefaultMediaPolicy(1888, 1);
    }

    @Bean
    public RatingPolicy ratingPolicy() {
        // rating 0..10 e permitir avaliação até 2 dias de diferença do watchedAt
        return new DefaultRatingPolicy(0, 10, 2);
    }

    @Bean
    public WatchEntryPolicy watchEntryPolicy() {
        // comment até 1000 chars; proíbe watchedAt no futuro
        return new DefaultWatchEntryPolicy(1000, true);
    }

    // ===== MEDIAS =====
    CreateMediaUseCase createMediaUseCase(MediaRepositoryPort repo, MediaPolicy mediaPolicy) {
        return new CreateMediaService(repo, mediaPolicy);
    }

    UpdateMediaUseCase updateMediaUseCase(MediaRepositoryPort repo, MediaPolicy mediaPolicy) {
        return new UpdateMediaService(repo);
    }

    GetMediaUseCase getMediaUseCase(MediaRepositoryPort repo) {
        return new GetMediaService(repo);
    }

    ListMediaUseCase listMediaUseCase(MediaRepositoryPort repo) {
        return new ListMediaService(repo);
    }

    DeleteMediaUseCase deleteMediaUseCase(MediaRepositoryPort repo) {
        return new DeleteMediaService(repo);
    }

    // ===== USERS =====

    public CreateUserUseCase createUserUseCase(UserRepositoryPort repo) {
        return new CreateUserService(repo);
    }

    public UpdateUserUseCase updateUserUseCase(UserRepositoryPort repo) {
        return new UpdateUserService(repo);
    }

    public GetUserUseCase getUserUseCase(UserRepositoryPort repo) {
        return new GetUserService(repo);
    }

    public ListUsersUseCase listUsersUseCase(UserRepositoryPort repo) {
        return new ListUsersService(repo);
    }

    public DeleteUserUseCase deleteUserUseCase(UserRepositoryPort repo) {
        return new DeleteUserService(repo);
    }

    // ===== GENRES =====

    public CreateGenreUseCase createGenreUseCase(GenreRepositoryPort repo) {
        return new CreateGenreService(repo);
    }

    public UpdateGenreUseCase updateGenreUseCase(GenreRepositoryPort repo) {
        return new UpdateGenreService(repo);
    }

    public GetGenreUseCase getGenreUseCase(GenreRepositoryPort repo) {
        return new GetGenreService(repo);
    }

    public ListGenresUseCase listGenresUseCase(GenreRepositoryPort repo) {
        return new ListGenresService(repo);
    }

    public DeleteGenreUseCase deleteGenreUseCase(GenreRepositoryPort repo) {
        return new DeleteGenreService(repo);
    }

    // ===== PEOPLE =====

    public CreatePersonUseCase createPersonUseCase(PersonRepositoryPort repo) {
        return new CreatePersonService(repo);
    }

    public UpdatePersonUseCase updatePersonUseCase(PersonRepositoryPort repo) {
        return new UpdatePersonService(repo);
    }

    public GetPersonUseCase getPersonUseCase(PersonRepositoryPort repo) {
        return new GetPersonService(repo);
    }

    public ListPeopleUseCase listPeopleUseCase(PersonRepositoryPort repo) {
        return new ListPeopleService(repo);
    }

    public DeletePersonUseCase deletePersonUseCase(PersonRepositoryPort repo) {
        return new DeletePersonService(repo);
    }

    // ===== CREDITS =====

    public CreateCreditUseCase createCreditUseCase(CreditRepositoryPort repo) {
        return new CreateCreditService(repo);
    }

    public UpdateCreditUseCase updateCreditUseCase(CreditRepositoryPort repo) {
        return new UpdateCreditService(repo);
    }

    public GetCreditUseCase getCreditUseCase(CreditRepositoryPort repo) {
        return new GetCreditService(repo);
    }

    public ListCreditsUseCase listCreditsUseCase(CreditRepositoryPort repo) {
        return new ListCreditsService(repo);
    }

    public DeleteCreditUseCase deleteCreditUseCase(CreditRepositoryPort repo) {
        return new DeleteCreditService(repo);
    }

    // ===== SEASONS =====

    public CreateSeasonUseCase createSeasonUseCase(SeasonRepositoryPort repo) {
        return new CreateSeasonService(repo);
    }

    public UpdateSeasonUseCase updateSeasonUseCase(SeasonRepositoryPort repo) {
        return new UpdateSeasonService(repo);
    }

    public GetSeasonUseCase getSeasonUseCase(SeasonRepositoryPort repo) {
        return new GetSeasonService(repo);
    }

    public ListSeasonsUseCase listSeasonsUseCase(SeasonRepositoryPort repo) {
        return new ListSeasonsService(repo);
    }

    public DeleteSeasonUseCase deleteSeasonUseCase(SeasonRepositoryPort repo) {
        return new DeleteSeasonService(repo);
    }

    // ===== EPISODES =====

    public CreateEpisodeUseCase createEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new CreateEpisodeService(repo);
    }

    public UpdateEpisodeUseCase updateEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new UpdateEpisodeService(repo);
    }

    public GetEpisodeUseCase getEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new GetEpisodeService(repo);
    }

    public ListEpisodesUseCase listEpisodesUseCase(EpisodeRepositoryPort repo) {
        return new ListEpisodesService(repo);
    }

    public DeleteEpisodeUseCase deleteEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new DeleteEpisodeService(repo);
    }

    // ===== WATCH ENTRIES =====

    public CreateWatchEntryUseCase createWatchEntryUseCase(WatchEntryRepositoryPort repo) {
        return new CreateWatchEntryService(repo);
    }

    public UpdateWatchEntryUseCase updateWatchEntryUseCase(WatchEntryRepositoryPort repo,
            WatchEntryPolicy watchEntryPolicy, RatingPolicy ratingPolicy) {
        return new UpdateWatchEntryService(repo, watchEntryPolicy, ratingPolicy);
    }

    public GetWatchEntryUseCase getWatchEntryUseCase(WatchEntryRepositoryPort repo) {
        return new GetWatchEntryService(repo);
    }

    public ListWatchEntriesUseCase listWatchEntriesUseCase(WatchEntryRepositoryPort repo) {
        return new ListWatchEntriesService(repo);
    }

    public DeleteWatchEntryUseCase deleteWatchEntryUseCase(WatchEntryRepositoryPort repo) {
        return new DeleteWatchEntryService(repo);
    }
}