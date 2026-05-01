package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.User;
import com.example.server.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<User>> list() {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        return ApiResponse.success(userService.listAll());
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody User req) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "邮箱不能为空");
        }
        if (userService.emailExists(req.getEmail())) {
            return ApiResponse.error(ErrorCode.EMAIL_EXISTS, "邮箱已存在");
        }
        userService.register(req);
        return ApiResponse.success(null);
    }

    @PutMapping("/{userId}")
    public ApiResponse<?> update(@PathVariable Long userId, @RequestBody User req) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        User updated = userService.updateUser(userId, req);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在或邮箱已被占用");
        }
        return ApiResponse.success(updated);
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<?> resetPassword(@PathVariable Long userId) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        if (!userService.resetPassword(userId)) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/{userId}/status")
    public ApiResponse<?> setStatus(@PathVariable Long userId, @RequestBody java.util.Map<String, String> body) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        String status = body.get("status");
        if (status == null || (!status.equals("ACTIVE") && !status.equals("DISABLED"))) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "状态值无效");
        }
        if (!userService.setUserStatus(userId, status)) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return ApiResponse.success(null);
    }
}
