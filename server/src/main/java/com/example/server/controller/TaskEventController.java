package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.TaskEvent;
import com.example.server.service.TaskEventService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/events")
public class TaskEventController {
    private final TaskEventService taskEventService;

    public TaskEventController(TaskEventService taskEventService) {
        this.taskEventService = taskEventService;
    }

    @GetMapping
    public ApiResponse<List<TaskEvent>> list(@RequestParam Long taskId) {
        return ApiResponse.success(taskEventService.queryByTaskId(taskId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }
}
