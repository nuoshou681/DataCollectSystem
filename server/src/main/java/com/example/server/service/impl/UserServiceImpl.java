package com.example.server.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.server.entity.User;
import com.example.server.mapper.UserMapper;
import com.example.server.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public boolean emailExists(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        return userMapper.selectOne(wrapper) != null;
    }

    @Override
    public void register(User req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(normalizeRole(req.getRole()));
        user.setStatus("ACTIVE");
        userMapper.insert(user);
    }

    @Override
    public User login(String email, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            if (passwordEncoder.matches(password, user.getPassword())) {
                UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("email", email).set("last_login_at", java.time.LocalDateTime.now());
                userMapper.update(null, updateWrapper);
                return user;
            }
            else if (password.equals(user.getPassword())) {
                String encoded = passwordEncoder.encode(password);
                UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("email", email).set("password", encoded).set("last_login_at", java.time.LocalDateTime.now());
                userMapper.update(null, updateWrapper);
                return user;
            }
        }
        return null;
    }

    @Override
    public List<User> listAll() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        return userMapper.selectList(wrapper);
    }

    @Override
    public User updateUser(Long userId, User req) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;
        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            user.setUsername(req.getUsername());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            if (!req.getEmail().equals(user.getEmail()) && emailExists(req.getEmail())) {
                return null;
            }
            user.setEmail(req.getEmail());
        }
        if (req.getRole() != null && !req.getRole().isBlank()) {
            user.setRole(normalizeRole(req.getRole()));
        }
        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    @Override
    public boolean resetPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.updateById(user);
        return true;
    }

    @Override
    public boolean setUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        user.setStatus(status);
        userMapper.updateById(user);
        return true;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        String trimmed = role.trim().toLowerCase();
        if ("admin".equals(trimmed)) {
            return "admin";
        }
        return "user";
    }
}
