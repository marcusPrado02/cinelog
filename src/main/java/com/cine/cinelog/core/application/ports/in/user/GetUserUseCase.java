package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;

public interface GetUserUseCase {
    User execute(Long id);
}
