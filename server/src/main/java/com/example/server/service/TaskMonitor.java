package com.example.server.service;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskMonitor {
    private final TaskRuntimeService taskRuntimeService;
    private final TaskEventService taskEventService;
    private final SystemConfigService systemConfigService;

    public TaskMonitor(TaskRuntimeService taskRuntimeService, TaskEventService taskEventService, SystemConfigService systemConfigService) {
        this.taskRuntimeService = taskRuntimeService;
        this.taskEventService = taskEventService;
        this.systemConfigService = systemConfigService;
    }

    @Scheduled(fixedDelay = 60000)
    public void markStalePendingTasks() {
        int timeoutMinutes = systemConfigService.getIntValue("task.queue.timeout.minutes", 10);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        taskRuntimeService.markPendingTimeout(cutoff);
    }
}
