package com.example.server.service;

import com.example.server.entity.User;
import java.util.List;

public interface UserService {
    boolean emailExists(String email);
    void register(User req);
    User login(String email, String password);
    List<User> listAll();
    User updateUser(Long userId, User req);
    boolean resetPassword(Long userId);
    boolean setUserStatus(Long userId, String status);
}
