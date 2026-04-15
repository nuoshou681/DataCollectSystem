package com.example.server.service;

import com.example.server.entity.User;

public interface UserService {
    boolean emailExists(String email);
    void register(User req);
    User login(String email, String password);
}
