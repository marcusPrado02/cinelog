package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.UpdateUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateUserService implements UpdateUserUseCase {
    private final UserRepositoryPort repo;

    public UpdateUserService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public User execute(Long id, User user) {
        var existing = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        return repo.save(existing);
    }
}
