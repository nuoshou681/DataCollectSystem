package com.example.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Task;
import com.example.server.entity.TaskLog;
import com.example.server.mapper.TaskLogMapper;
import com.example.server.mapper.TaskMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
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
                return emptyPage();
            }
            queryWrapper.in(TaskLog::getTaskId, taskIds);
        }
        if (level != null && !level.isBlank() && !"ALL".equalsIgnoreCase(level)) {
            queryWrapper.eq(TaskLog::getLogLevel, level.toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.and(w -> w
                    .like(TaskLog::getLogMessage, keyword)
                    .or()
                    .like(TaskLog::getNodeKey, keyword));
        }
        queryWrapper.orderByDesc(TaskLog::getLogId);

        long total = taskLogMapper.selectCount(queryWrapper);
        List<TaskLog> records = taskLogMapper.selectList(
                queryWrapper.last("LIMIT " + ((page - 1) * size) + ", " + size));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return ApiResponse.success(data);
    }

    private ApiResponse<Map<String, Object>> emptyPage() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", List.of());
        data.put("total", 0L);
        data.put("page", 1L);
        data.put("size", 20L);
        return ApiResponse.success(data);
    }
}
