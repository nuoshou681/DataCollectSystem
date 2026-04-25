package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.service.TaskRuntimeStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/task/runtime")
public class TaskRuntimeController {
    private final TaskRuntimeStreamService taskRuntimeStreamService;

    public TaskRuntimeController(TaskRuntimeStreamService taskRuntimeStreamService) {
        this.taskRuntimeStreamService = taskRuntimeStreamService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return taskRuntimeStreamService.subscribe(SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
    }
}
