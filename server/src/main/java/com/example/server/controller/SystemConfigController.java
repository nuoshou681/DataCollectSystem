package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.SystemConfig;
import com.example.server.service.SystemConfigService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/config")
public class SystemConfigController {
    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ApiResponse<java.util.List<SystemConfig>> list() {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        return ApiResponse.success(systemConfigService.list());
    }

    @PutMapping("/{key}")
    public ApiResponse<?> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        String value = body.get("value");
        SystemConfig updated = systemConfigService.update(key, value);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "配置项不存在");
        }
        return ApiResponse.success(updated);
    }
}
