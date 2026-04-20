package com.example.server.service.impl;

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
        user.setPassword(passwordEncoder.encode(req.getPassword())); // 密码加密
        user.setRole(normalizeRole(req.getRole()));
        userMapper.insert(user);
    }

    @Override
    public User login(String email, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        User user = userMapper.selectOne(wrapper);
        if (user != null) {
            // 1. 加密密码校验
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
            // 2. 明文密码校验
            else if (password.equals(user.getPassword())) {
                String encoded = passwordEncoder.encode(password);
                UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("email", email).set("password", encoded);
                userMapper.update(null, updateWrapper);
                return user;
            }
        }
        return null;
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
