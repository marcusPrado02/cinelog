package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.DeleteUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteUserService implements DeleteUserUseCase {
    private final UserRepositoryPort repo;

    public DeleteUserService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}