package com.example.server.service;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskMonitor {
    private final TaskRuntimeService taskRuntimeService;
    private final TaskEventService taskEventService;

    public TaskMonitor(TaskRuntimeService taskRuntimeService, TaskEventService taskEventService) {
        this.taskRuntimeService = taskRuntimeService;
        this.taskEventService = taskEventService;
    }

    @Scheduled(fixedDelay = 60000)
    public void markStalePendingTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        taskRuntimeService.markPendingTimeout(cutoff);
    }
}
