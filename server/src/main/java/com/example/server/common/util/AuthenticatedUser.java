package com.example.server.common.util;

public record AuthenticatedUser(Long userId, String email, String role) {
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
