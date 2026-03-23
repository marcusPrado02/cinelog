package com.cine.cinelog.features.batch.jobs.people;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.core.domain.model.tmdb.TmdbPersonDetails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

/**
 * Busca detalhes da pessoa no TMDB e enriquece o domínio Person com
 * biography, profileUrl, knownForDepartment, gender, popularity, imdbId,
 * homepage e deathday.
 */
@Slf4j
@Component
public class TmdbPersonDetailsProcessor implements ItemProcessor<Person, Person> {

    private final TmdbClientPort tmdbClient;

    public TmdbPersonDetailsProcessor(TmdbClientPort tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    @Override
    public Person process(Person person) {
        if (person.getTmdbPersonId() == null) {
            return null;
        }

        try {
            Optional<TmdbPersonDetails> detailsOpt = tmdbClient.fetchPersonDetails(person.getTmdbPersonId());

            if (detailsOpt.isEmpty()) {
                return null;
            }

            TmdbPersonDetails details = detailsOpt.get();

            if (details.getProfilePath() != null) {
                person.setProfileUrl(details.getProfilePath());
            }
            if (details.getBiography() != null && !details.getBiography().isBlank()) {
                person.setBiography(details.getBiography());
            }
            if (details.getKnownForDepartment() != null) {
                person.setKnownForDepartment(details.getKnownForDepartment());
            }
            if (details.getGender() != null) {
                person.setGender(details.getGender());
            }
            if (details.getPopularity() != null) {
                person.setPopularity(details.getPopularity());
            }
            if (details.getImdbId() != null) {
                person.setImdbId(details.getImdbId());
            }
            if (details.getHomepage() != null) {
                person.setHomepage(details.getHomepage());
            }
            if (details.getDeathday() != null) {
                person.setDeathday(details.getDeathday());
            }

            return person;

        } catch (WebClientResponseException | RestClientException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Falha ao buscar detalhes para pessoa id={} tmdbPersonId={}: {}",
                    person.getId(), person.getTmdbPersonId(), e.getMessage());
            return null;
        }
    }
}
