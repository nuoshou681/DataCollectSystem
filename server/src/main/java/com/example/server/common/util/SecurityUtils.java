package com.example.server.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        return null;
    }

    public static Long getCurrentUserId() {
        AuthenticatedUser user = getCurrentUser();
        return user == null ? null : user.userId();
    }

    public static boolean isAdmin() {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.isAdmin();
    }
}
