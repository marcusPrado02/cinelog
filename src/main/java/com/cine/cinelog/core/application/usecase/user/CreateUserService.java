package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.in.user.CreateUserUseCase;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateUserService implements CreateUserUseCase {

    private final UserRepositoryPort userRepo;

    public CreateUserService(UserRepositoryPort userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User execute(User user) {
        return userRepo.save(user);
    }
}
