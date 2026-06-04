package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.Notification;
import com.example.server.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<Notification>> list(@RequestParam(required = false) Boolean unread) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (SecurityUtils.isAdmin()) {
            return ApiResponse.success(notificationService.listAll(unread));
        }
        return ApiResponse.success(notificationService.listByUser(userId, unread));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (SecurityUtils.isAdmin()) {
            return ApiResponse.success(notificationService.unreadCountAll());
        }
        return ApiResponse.success(notificationService.unreadCount(userId));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<?> markRead(@PathVariable Long id) {
        if (SecurityUtils.isAdmin()) {
            notificationService.markReadAny(id);
        } else {
            notificationService.markRead(id, SecurityUtils.getCurrentUserId());
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/read-all")
    public ApiResponse<?> markAllRead() {
        if (SecurityUtils.isAdmin()) {
            notificationService.markAllReadAny();
        } else {
            notificationService.markAllRead(SecurityUtils.getCurrentUserId());
        }
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        boolean ok = SecurityUtils.isAdmin()
                ? notificationService.deleteAny(id)
                : notificationService.delete(id, SecurityUtils.getCurrentUserId());
        if (!ok) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "通知不存在");
        }
        return ApiResponse.success(null);
    }
}
