package com.cine.cinelog.shared.security;

import java.util.Set;

public record AuthenticatedUser(
        Long id,
        String email,
        Set<String> roles) {

    public boolean hasRole(String role) {
        if (roles == null)
            return false;
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}