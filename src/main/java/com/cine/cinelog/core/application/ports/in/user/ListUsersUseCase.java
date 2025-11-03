package com.cine.cinelog.core.application.ports.in.user;

import com.cine.cinelog.core.domain.model.User;
import java.util.List;

public interface ListUsersUseCase {
    List<User> execute();
}