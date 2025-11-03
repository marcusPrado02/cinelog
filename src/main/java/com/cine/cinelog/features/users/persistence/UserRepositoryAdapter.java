package com.cine.cinelog.features.users.persistence;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.features.users.mapper.UserMapper;
import com.cine.cinelog.features.users.repository.UserJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpa;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(UserJpaRepository jpa, UserMapper userMapper) {
        this.jpa = jpa;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        var entity = userMapper.toEntity(user);
        var saved = jpa.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(userMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll().stream().map(userMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
