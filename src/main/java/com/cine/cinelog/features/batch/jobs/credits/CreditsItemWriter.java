package com.cine.cinelog.features.batch.jobs.credits;

import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.enums.Role;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writer que persiste créditos no banco de dados.
 *
 * <p>
 * Para cada bundle: faz upsert de Person por tmdbPersonId e insere
 * Credit se ainda não existir para (mediaId, personId, role).
 * </p>
 */
@Component
public class CreditsItemWriter implements ItemWriter<TmdbCreditsBundle> {

    private static final Logger log = LoggerFactory.getLogger(CreditsItemWriter.class);

    private final PersonRepositoryPort personRepository;
    private final CreditRepositoryPort creditRepository;

    public CreditsItemWriter(PersonRepositoryPort personRepository, CreditRepositoryPort creditRepository) {
        this.personRepository = personRepository;
        this.creditRepository = creditRepository;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends TmdbCreditsBundle> chunk) {
        for (TmdbCreditsBundle bundle : chunk) {
            processCast(bundle);
            processCrew(bundle);
        }
    }

    private void processCast(TmdbCreditsBundle bundle) {
        List<TmdbCredits.CastMember> cast = bundle.getCredits().getCast();
        if (cast == null)
            return;

        int idx = 0;
        for (TmdbCredits.CastMember member : cast) {
            if (member.getTmdbPersonId() == null || member.getName() == null)
                continue;
            try {
                Person person = upsertPerson(member.getTmdbPersonId(), member.getName(), member.getProfileUrl());
                String role = Role.ACTOR.name();
                if (!creditRepository.existsByMediaIdAndPersonIdAndRole(bundle.getMediaId(), person.getId(), role)) {
                    Credit credit = new Credit();
                    credit.setMediaId(bundle.getMediaId());
                    credit.setPersonId(person.getId());
                    credit.setRole(Role.ACTOR);
                    credit.setCharacterName(member.getCharacter());
                    credit.setOrderIndex((short) Math.min(idx, Short.MAX_VALUE));
                    creditRepository.save(credit);
                }
                idx++;
            } catch (Exception e) {
                log.warn("Erro ao processar cast member '{}': {}", member.getName(), e.getMessage());
            }
        }
    }

    private void processCrew(TmdbCreditsBundle bundle) {
        List<TmdbCredits.CrewMember> crew = bundle.getCredits().getCrew();
        if (crew == null)
            return;

        for (TmdbCredits.CrewMember member : crew) {
            if (member.getTmdbPersonId() == null || member.getName() == null)
                continue;
            Role role = mapJobToRole(member.getJob());
            if (role == null)
                continue;

            try {
                Person person = upsertPerson(member.getTmdbPersonId(), member.getName(), member.getProfileUrl());
                String roleStr = role.name();
                if (!creditRepository.existsByMediaIdAndPersonIdAndRole(bundle.getMediaId(), person.getId(), roleStr)) {
                    Credit credit = new Credit();
                    credit.setMediaId(bundle.getMediaId());
                    credit.setPersonId(person.getId());
                    credit.setRole(role);
                    creditRepository.save(credit);
                }
            } catch (Exception e) {
                log.warn("Erro ao processar crew member '{}': {}", member.getName(), e.getMessage());
            }
        }
    }

    private Person upsertPerson(Long tmdbPersonId, String name, String profileUrl) {
        return personRepository.findByTmdbPersonId(tmdbPersonId)
                .map(existing -> {
                    if (profileUrl != null
                            && (existing.getProfileUrl() == null || existing.getProfileUrl().isBlank())) {
                        existing.setProfileUrl(profileUrl);
                        return personRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Person p = new Person();
                    p.setName(name);
                    p.setTmdbPersonId(tmdbPersonId);
                    p.setProfileUrl(profileUrl);
                    return personRepository.save(p);
                });
    }

    private Role mapJobToRole(String job) {
        if (job == null)
            return null;
        return switch (job.toLowerCase()) {
            case "director" -> Role.DIRECTOR;
            case "writer", "screenplay", "story" -> Role.WRITER;
            case "producer", "executive producer" -> Role.PRODUCER;
            case "original music composer", "music" -> Role.COMPOSER;
            default -> null;
        };
    }
}
