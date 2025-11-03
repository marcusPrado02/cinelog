package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;

public interface CreateUserUseCase {
    User execute(User user);
}
