package com.cine.cinelog.core.application.usecase.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaPolicy;

@Service
@Transactional
public class CreateMediaService implements CreateMediaUseCase {

    private final MediaRepositoryPort repo;
    private final MediaPolicy policy;

    public CreateMediaService(MediaRepositoryPort repo, MediaPolicy policy) {
        this.repo = repo;
        this.policy = policy;
    }

    @Override
    public Media execute(Media media) {
        media.normalize();
        media.validateInvariants();
        return repo.save(media);
    }
}