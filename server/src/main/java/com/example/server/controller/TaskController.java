package com.example.server.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.example.server.entity.Task;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.TaskService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private TaskMapper userTaskMapper;

    @Autowired
    private TaskService taskService;

    @GetMapping("/task")
    public ApiResponse<List<Task>> queryList() {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Task::getTaskId);
        return ApiResponse.success(userTaskMapper.selectList(queryWrapper));
    }

    @PostMapping("/dispatch") // 实际路径: /task/dispatch
    public ApiResponse<?> dispatchTask(@RequestBody Task task) {
        if (task == null || task.getKeyword() == null || task.getKeyword().isBlank() || task.getUrl() == null
                || task.getUrl().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "关键词和网站地址不能为空");
        }

        if (task.getStatus() == null || task.getStatus().isBlank()) {
            task.setStatus("PENDING");
        }
        if (task.getProgress() == null) {
            task.setProgress(0);
        }

        try {
            taskService.splitAndDispatchSubTasks(task);
            return ApiResponse.success(task.getTaskId());
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.SERVER_ERROR, "任务分发失败: " + e.getMessage());
        }
    }

}
