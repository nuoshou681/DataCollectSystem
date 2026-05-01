package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.TaskSchedule;
import com.example.server.service.TaskScheduleService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/schedules")
public class TaskScheduleController {
    private final TaskScheduleService scheduleService;

    public TaskScheduleController(TaskScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ApiResponse<List<TaskSchedule>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(scheduleService.listByUser(userId));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody TaskSchedule s) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (s.getScheduleName() == null || s.getScheduleName().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "调度名称不能为空");
        }
        if (s.getKeyword() == null || s.getKeyword().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "关键词不能为空");
        }
        if (s.getCronExpression() == null || s.getCronExpression().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "执行间隔不能为空");
        }
        s.setUserId(userId);
        return ApiResponse.success(scheduleService.create(s));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody TaskSchedule s) {
        Long userId = SecurityUtils.getCurrentUserId();
        s.setScheduleId(id);
        TaskSchedule updated = scheduleService.update(s, userId);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "调度不存在或无权修改");
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!scheduleService.delete(id, userId)) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "调度不存在");
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/toggle")
    public ApiResponse<?> toggle(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!scheduleService.toggle(id, userId)) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "调度不存在");
        }
        return ApiResponse.success(null);
    }
}
