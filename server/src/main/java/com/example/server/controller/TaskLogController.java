package com.example.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Task;
import com.example.server.entity.TaskLog;
import com.example.server.mapper.TaskLogMapper;
import com.example.server.mapper.TaskMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskLogController {
    private final TaskLogMapper taskLogMapper;
    private final TaskMapper taskMapper;

    public TaskLogController(TaskLogMapper taskLogMapper, TaskMapper taskMapper) {
        this.taskLogMapper = taskLogMapper;
        this.taskMapper = taskMapper;
    }

    @GetMapping("/log")
    public ApiResponse<List<TaskLog>> list() {
        LambdaQueryWrapper<TaskLog> queryWrapper = new LambdaQueryWrapper<>();
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.getCurrentUserId();
            List<Long> taskIds = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                            .eq(Task::getUserId, userId)
                            .select(Task::getTaskId))
                    .stream()
                    .map(Task::getTaskId)
                    .collect(Collectors.toList());
            if (taskIds.isEmpty()) {
                return ApiResponse.success(List.of());
            }
            queryWrapper.in(TaskLog::getTaskId, taskIds);
        }
        queryWrapper.orderByDesc(TaskLog::getLogId);
        return ApiResponse.success(taskLogMapper.selectList(queryWrapper));
    }
}
