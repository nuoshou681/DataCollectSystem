package com.example.server.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskMonitor {
    private final TaskMapper taskMapper;

    public TaskMonitor(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Scheduled(fixedDelay = 60000)
    public void markStalePendingTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Task::getTaskStatus, "PENDING")
                .lt(Task::getCreatedAt, cutoff)
                .set(Task::getTaskStatus, "FAILED")
                .set(Task::getTaskProgress, 100)
                .set(Task::getUpdatedAt, LocalDateTime.now());
        taskMapper.update(null, updateWrapper);
    }
}
