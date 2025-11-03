package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.GetUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetUserService implements GetUserUseCase {
    private final UserRepositoryPort repo;

    public GetUserService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public User execute(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}