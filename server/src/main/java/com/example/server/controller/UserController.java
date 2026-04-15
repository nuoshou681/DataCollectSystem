package com.example.server.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.server.common.util.JwtUtil;
import com.example.server.entity.User;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    // 注册接口
    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody User reqUser) {
        if (userService.emailExists(reqUser.getEmail())) {
            return ApiResponse.error(ErrorCode.EMAIL_EXISTS, "邮箱已存在");
        }
        userService.register(reqUser);
        return ApiResponse.success(null);
    }

    // 登陆接口
    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody User reqUser) {
        User user = userService.login(reqUser.getEmail(), reqUser.getPassword());
        if (user == null) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "邮箱或密码错误");
        }
        String token = jwtUtil.generateToken(user);
        return ApiResponse.success(token);
    }

}