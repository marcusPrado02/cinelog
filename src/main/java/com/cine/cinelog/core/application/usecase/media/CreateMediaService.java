package com.cine.cinelog.core.application.usecase.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.ports.in.media.CreateMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;

@Service
@Transactional
public class CreateMediaService implements CreateMediaUseCase {

    private final MediaRepositoryPort repo;

    public CreateMediaService(MediaRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Media execute(Media input) {
        // Valida as invariantes de domínio antes de persistir.
        // Lance exceção em caso de dados inválidos.
        input.validateInvariants();
        // Delegação à porta de persistência para salvar a entidade.
        return repo.save(input);
    }
}