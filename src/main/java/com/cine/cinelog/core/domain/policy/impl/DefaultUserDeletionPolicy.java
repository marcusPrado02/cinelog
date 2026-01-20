package com.cine.cinelog.core.domain.policy.impl;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.application.ports.out.WatchlistRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserDeletionPolicy;
/**
 * Política de domínio para gerenciamento de defaultuserdeletion.
 * Define as regras e validações relacionadas a defaultuserdeletion.
 * 
 * <p>Esta política encapsula lógica de negócio específica
 * e é aplicada durante operações em DefaultUserDeletion.</p>
 * 
 * @since 1.0
 * @see DefaultUserDeletion
 */

@Component
public class DefaultUserDeletionPolicy implements UserDeletionPolicy {

    private final WatchEntryRepositoryPort watchEntryRepo;
    private final WatchlistRepositoryPort watchlistRepo;

    public DefaultUserDeletionPolicy(WatchEntryRepositoryPort watchEntryRepo,
            WatchlistRepositoryPort watchlistRepo) {
        this.watchEntryRepo = watchEntryRepo;
        this.watchlistRepo = watchlistRepo;
    }

    @Override
    public void validateDelete(User user) {
        if (user == null || user.getId() == null) {
            throw DomainException.of(ErrorCode.USER_DELETE_FORBIDDEN);
        }

        Long userId = user.getId();

        boolean hasWatchEntries = watchEntryRepo.existsByUserId(userId);
        boolean hasWatchlistItems = watchlistRepo.existsByUserId(userId);

        if (!hasWatchEntries && !hasWatchlistItems) {
            return; // pode deletar
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("userId", userId);
        details.put("hasWatchEntries", hasWatchEntries);
        details.put("hasWatchlistItems", hasWatchlistItems);

        throw DomainException.of(ErrorCode.USER_DELETE_FORBIDDEN, details);
    }
}
