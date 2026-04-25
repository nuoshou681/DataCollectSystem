package com.example.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.TaskRuntimeService;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/maintenance")
public class TaskMaintenanceController {
    private final TaskMapper taskMapper;
    private final TaskRuntimeService taskRuntimeService;

    public TaskMaintenanceController(TaskMapper taskMapper, TaskRuntimeService taskRuntimeService) {
        this.taskMapper = taskMapper;
        this.taskRuntimeService = taskRuntimeService;
    }

    @PostMapping("/recount")
    public ApiResponse<?> recountTaskRuntime() {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.UNAUTHORIZED, "仅管理员可执行");
        }

        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>().orderByAsc(Task::getTaskId));
        for (Task task : tasks) {
            taskRuntimeService.recountFromPageResults(task.getTaskId());
        }
        return ApiResponse.success("ok");
    }
}
