package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.ListUsersUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListUsersService implements ListUsersUseCase {
    private final UserRepositoryPort repo;

    public ListUsersService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<User> execute() {
        return repo.findAll();
    }
}