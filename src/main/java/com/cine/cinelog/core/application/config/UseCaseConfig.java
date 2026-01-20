package com.cine.cinelog.core.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cine.cinelog.core.application.ports.events.DomainEventPublisherPort;
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
import com.cine.cinelog.core.application.ports.in.media.RecommendMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.SearchMediaUseCase;
import com.cine.cinelog.core.application.ports.in.media.SyncMediaFromTmdbUseCase;
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
import com.cine.cinelog.core.application.ports.in.security.CurrentUserProvider;
import com.cine.cinelog.core.application.ports.in.user.CreateUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.DeleteUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.GetMyStatsUseCase;
import com.cine.cinelog.core.application.ports.in.user.GetUserUseCase;
import com.cine.cinelog.core.application.ports.in.user.ListUsersUseCase;
import com.cine.cinelog.core.application.ports.in.user.UpdateUserUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.CreateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.DeleteWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.GetWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.ListWatchEntriesUseCase;
import com.cine.cinelog.core.application.ports.in.watchentry.UpdateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.in.watchlist.AddToWatchlistUseCase;
import com.cine.cinelog.core.application.ports.in.watchlist.ListMyWatchlistUseCase;
import com.cine.cinelog.core.application.ports.in.watchlist.RemoveFromWatchlistUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.application.ports.out.MediaGenreLinkPort;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
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
import com.cine.cinelog.core.application.usecase.media.RecommendMediaService;
import com.cine.cinelog.core.application.usecase.media.SearchMediaService;
import com.cine.cinelog.core.application.usecase.media.SyncMediaFromTmdbService;
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
import com.cine.cinelog.core.application.usecase.user.GetMyStatsService;
import com.cine.cinelog.core.application.usecase.user.GetUserService;
import com.cine.cinelog.core.application.usecase.user.ListUsersService;
import com.cine.cinelog.core.application.usecase.user.UpdateUserService;
import com.cine.cinelog.core.application.usecase.watchentry.GetWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.ListWatchEntriesService;
import com.cine.cinelog.core.application.usecase.watchentry.CreateWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.DeleteWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchentry.UpdateWatchEntryService;
import com.cine.cinelog.core.application.usecase.watchlist.AddToWatchlistService;
import com.cine.cinelog.core.application.usecase.watchlist.ListMyWatchlistService;
import com.cine.cinelog.core.application.usecase.watchlist.RemoveFromWatchlistService;
import com.cine.cinelog.core.domain.policy.MediaDeletionPolicy;
import com.cine.cinelog.core.domain.policy.MediaPolicy;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.SeasonDeletionPolicy;
import com.cine.cinelog.core.domain.policy.SeasonPolicy;
import com.cine.cinelog.core.domain.policy.SeasonUniquenessPolicy;
import com.cine.cinelog.core.domain.policy.UserDeletionPolicy;
import com.cine.cinelog.core.domain.policy.UserEmailUniquenessPolicy;
import com.cine.cinelog.core.domain.policy.UserPolicy;
import com.cine.cinelog.core.domain.policy.UserUpdatePolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryReferencePolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryUniquenessPolicy;
import com.cine.cinelog.core.domain.policy.WatchlistReferencePolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultMediaPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultRatingPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultWatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.impl.DefaultWatchlistReferencePolicy;

/**
 * Configuração dos casos de uso da aplicação.
 *
 * Cada método cria e expõe um bean correspondente a um caso de uso,
 * injetando a porta de repositório necessária.
 */
/**
 * Serviço responsável por operação de UseCase.
 * Implementa o caso de uso de operação aplicando regras de negócio e políticas
 * de domínio.
 *
 * <p>
 * Este serviço coordena as operações necessárias e garante
 * a consistência dos dados através de validações e políticas.
 * </p>
 *
 * @since 1.0
 * @see UseCaseConfig
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
        return new DefaultWatchEntryPolicy();
    }

    @Bean
    public WatchlistReferencePolicy watchlistReferencePolicy(MediaRepositoryPort mediaRepo) {
        // permitir até 5 anos no futuro
        return new DefaultWatchlistReferencePolicy(mediaRepo, 5);
    }

    // ===== MEDIAS =====
    @Bean
    public CreateMediaUseCase createMediaUseCase(MediaRepositoryPort repo, MediaPolicy mediaPolicy) {
        return new CreateMediaService(repo, mediaPolicy);
    }

    @Bean
    public UpdateMediaUseCase updateMediaUseCase(MediaRepositoryPort repo, MediaPolicy mediaPolicy) {
        return new UpdateMediaService(repo, mediaPolicy);
    }

    @Bean
    public GetMediaUseCase getMediaUseCase(MediaRepositoryPort repo) {
        return new GetMediaService(repo);
    }

    @Bean
    public ListMediaUseCase listMediaUseCase(MediaRepositoryPort repo) {
        return new ListMediaService(repo);
    }

    @Bean
    public DeleteMediaUseCase deleteMediaUseCase(MediaRepositoryPort repo, MediaDeletionPolicy deletionPolicy) {
        return new DeleteMediaService(repo, deletionPolicy);
    }

    @Bean
    public SearchMediaUseCase searchMediaUseCase(MediaRepositoryPort repo) {
        return new SearchMediaService(repo);
    }

    @Bean
    public RecommendMediaUseCase recommendMediaUseCase(MediaRepositoryPort repo) {
        return new RecommendMediaService(repo);
    }

    @Bean
    public SyncMediaFromTmdbUseCase syncMediaFromTmdbUseCase(MediaRepositoryPort mediaRepository,
            TmdbClientPort tmdbClient, GenreRepositoryPort genreRepository, MediaGenreLinkPort mediaGenreLinkPort) {
        return new SyncMediaFromTmdbService(mediaRepository, tmdbClient, genreRepository, mediaGenreLinkPort);
    }

    // ===== USERS =====

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepositoryPort repo, UserPolicy userPolicy,
            UserEmailUniquenessPolicy uniqueness) {
        return new CreateUserService(repo, userPolicy, uniqueness);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepositoryPort repo, UserPolicy userPolicy,
            UserUpdatePolicy updatePolicy, UserEmailUniquenessPolicy uniqueness) {
        return new UpdateUserService(repo, userPolicy, updatePolicy, uniqueness);
    }

    @Bean
    public GetUserUseCase getUserUseCase(UserRepositoryPort repo) {
        return new GetUserService(repo);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepositoryPort repo) {
        return new ListUsersService(repo);
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserRepositoryPort repo, UserDeletionPolicy deletionPolicy) {
        return new DeleteUserService(repo, deletionPolicy);
    }

    // ===== GENRES =====

    @Bean
    public CreateGenreUseCase createGenreUseCase(GenreRepositoryPort repo) {
        return new CreateGenreService(repo);
    }

    @Bean
    public UpdateGenreUseCase updateGenreUseCase(GenreRepositoryPort repo) {
        return new UpdateGenreService(repo);
    }

    @Bean
    public GetGenreUseCase getGenreUseCase(GenreRepositoryPort repo) {
        return new GetGenreService(repo);
    }

    @Bean
    public ListGenresUseCase listGenresUseCase(GenreRepositoryPort repo) {
        return new ListGenresService(repo);
    }

    @Bean
    public DeleteGenreUseCase deleteGenreUseCase(GenreRepositoryPort repo) {
        return new DeleteGenreService(repo);
    }

    // ===== PEOPLE =====

    @Bean
    public CreatePersonUseCase createPersonUseCase(PersonRepositoryPort repo) {
        return new CreatePersonService(repo);
    }

    @Bean
    public UpdatePersonUseCase updatePersonUseCase(PersonRepositoryPort repo) {
        return new UpdatePersonService(repo);
    }

    @Bean
    public GetPersonUseCase getPersonUseCase(PersonRepositoryPort repo) {
        return new GetPersonService(repo);
    }

    @Bean
    public ListPeopleUseCase listPeopleUseCase(PersonRepositoryPort repo) {
        return new ListPeopleService(repo);
    }

    @Bean
    public DeletePersonUseCase deletePersonUseCase(PersonRepositoryPort repo) {
        return new DeletePersonService(repo);
    }

    // ===== CREDITS =====

    @Bean
    public CreateCreditUseCase createCreditUseCase(CreditRepositoryPort repo) {
        return new CreateCreditService(repo);
    }

    @Bean
    public UpdateCreditUseCase updateCreditUseCase(CreditRepositoryPort repo) {
        return new UpdateCreditService(repo);
    }

    @Bean
    public GetCreditUseCase getCreditUseCase(CreditRepositoryPort repo) {
        return new GetCreditService(repo);
    }

    @Bean
    public ListCreditsUseCase listCreditsUseCase(CreditRepositoryPort repo) {
        return new ListCreditsService(repo);
    }

    @Bean
    public DeleteCreditUseCase deleteCreditUseCase(CreditRepositoryPort repo) {
        return new DeleteCreditService(repo);
    }

    // ===== SEASONS =====

    @Bean
    public CreateSeasonUseCase createSeasonUseCase(SeasonRepositoryPort repo, SeasonPolicy policy,
            SeasonUniquenessPolicy uniquenessPolicy) {
        return new CreateSeasonService(repo, policy, uniquenessPolicy);
    }

    @Bean
    public UpdateSeasonUseCase updateSeasonUseCase(SeasonRepositoryPort repo, SeasonPolicy policy) {
        return new UpdateSeasonService(repo);
    }

    @Bean
    public GetSeasonUseCase getSeasonUseCase(SeasonRepositoryPort repo) {
        return new GetSeasonService(repo);
    }

    @Bean
    public ListSeasonsUseCase listSeasonsUseCase(SeasonRepositoryPort repo) {
        return new ListSeasonsService(repo);
    }

    @Bean
    public DeleteSeasonUseCase deleteSeasonUseCase(SeasonRepositoryPort repo, SeasonDeletionPolicy deletionPolicy) {
        return new DeleteSeasonService(repo, deletionPolicy);
    }

    // ===== EPISODES =====

    @Bean
    public CreateEpisodeUseCase createEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new CreateEpisodeService(repo);
    }

    @Bean
    public UpdateEpisodeUseCase updateEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new UpdateEpisodeService(repo);
    }

    @Bean
    public GetEpisodeUseCase getEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new GetEpisodeService(repo);
    }

    @Bean
    public ListEpisodesUseCase listEpisodesUseCase(EpisodeRepositoryPort repo) {
        return new ListEpisodesService(repo);
    }

    @Bean
    public DeleteEpisodeUseCase deleteEpisodeUseCase(EpisodeRepositoryPort repo) {
        return new DeleteEpisodeService(repo);
    }

    // ===== WATCH ENTRIES =====

    @Bean
    public CreateWatchEntryUseCase createWatchEntryUseCase(
            WatchEntryRepositoryPort repo,
            WatchEntryPolicy policy,
            WatchEntryUniquenessPolicy uniquenessPolicy,
            DomainEventPublisherPort eventPublisher) {
        return new CreateWatchEntryService(repo, policy, uniquenessPolicy, eventPublisher);
    }

    @Bean
    public UpdateWatchEntryUseCase updateWatchEntryUseCase(WatchEntryRepositoryPort repo,
            WatchEntryPolicy watchEntryPolicy, RatingPolicy ratingPolicy, WatchEntryReferencePolicy referencePolicy) {
        return new UpdateWatchEntryService(repo, watchEntryPolicy, ratingPolicy, referencePolicy);
    }

    @Bean
    public GetWatchEntryUseCase getWatchEntryUseCase(WatchEntryRepositoryPort repo) {
        return new GetWatchEntryService(repo);
    }

    @Bean
    public ListWatchEntriesUseCase listWatchEntriesUseCase(WatchEntryRepositoryPort repo) {
        return new ListWatchEntriesService(repo);
    }

    @Bean
    public DeleteWatchEntryUseCase deleteWatchEntryUseCase(WatchEntryRepositoryPort repo) {
        return new DeleteWatchEntryService(repo);
    }

    @Bean
    public GetMyStatsUseCase getMyStatsUseCase(WatchEntryRepositoryPort repo, CurrentUserProvider currentUserProvider) {
        return new GetMyStatsService(repo, currentUserProvider);
    }

    // ===== WATCHLIST =====

    @Bean
    public AddToWatchlistUseCase addToWatchlistUseCase(WatchlistRepositoryPort repository,
            CurrentUserProvider currentUser, WatchlistReferencePolicy referencePolicy) {
        return new AddToWatchlistService(repository, currentUser, referencePolicy);
    }

    @Bean
    public ListMyWatchlistUseCase listMyWatchlistUseCase(WatchlistRepositoryPort repository,
            CurrentUserProvider currentUser) {
        return new ListMyWatchlistService(repository, currentUser);
    }

    @Bean
    public RemoveFromWatchlistUseCase removeFromWatchlistUseCase(WatchlistRepositoryPort repository,
            CurrentUserProvider currentUser) {
        return new RemoveFromWatchlistService(repository, currentUser);
    }
}
