package com.example.server.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.example.server.entity.DispatchTaskRequest;
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
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskService taskService;

    @GetMapping("/task")
    public ApiResponse<List<Task>> queryList() {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Task::getTaskId);
        return ApiResponse.success(taskMapper.selectList(queryWrapper));
    }

    @PostMapping("/dispatch") // 实际路径: /task/dispatch
    public ApiResponse<?> dispatchTask(@RequestBody DispatchTaskRequest request) {
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()
                || request.getUrl() == null
                || request.getUrl().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "关键词和网站地址不能为空");
        }

        try {
            List<Task> tasks = taskService.dispatchTasks(request);
            return ApiResponse.success(tasks);
        } catch (Exception e) {
            log.error("任务分发失败, keyword={}, url={}", request.getKeyword(), request.getUrl(), e);
            return ApiResponse.error(ErrorCode.SERVER_ERROR, "任务分发失败: " + e.getMessage());
        }
    }

}
