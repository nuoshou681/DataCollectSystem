package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.Crawler;
import com.example.server.mapper.ClientMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskMonitor {
    private final TaskRuntimeService taskRuntimeService;
    private final TaskEventService taskEventService;
    private final SystemConfigService systemConfigService;
    private final ClientMapper clientMapper;

    public TaskMonitor(TaskRuntimeService taskRuntimeService, TaskEventService taskEventService, SystemConfigService systemConfigService, ClientMapper clientMapper) {
        this.taskRuntimeService = taskRuntimeService;
        this.taskEventService = taskEventService;
        this.systemConfigService = systemConfigService;
        this.clientMapper = clientMapper;
    }

    @Scheduled(fixedDelay = 60000)
    public void markStalePendingTasks() {
        int timeoutMinutes = systemConfigService.getIntValue("task.queue.timeout.minutes", 10);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        taskRuntimeService.markPendingTimeout(cutoff);
    }

    @Scheduled(fixedDelay = 120000)
    public void markStaleRunningTasks() {
        int timeoutMinutes = systemConfigService.getIntValue("task.running.timeout.minutes", 30);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        taskRuntimeService.markRunningTimeout(cutoff);
    }

    @Scheduled(fixedDelay = 30000)
    public void markOrphanedNodeTasks() {
        List<Crawler> nodes = clientMapper.selectList(new LambdaQueryWrapper<>());
        for (Crawler node : nodes) {
            if (node.getLastHeartbeat() == null) continue;
            int timeoutSec = node.getHeartbeatTimeoutSec() != null ? node.getHeartbeatTimeoutSec() : 15;
            LocalDateTime deadline = LocalDateTime.now().minusSeconds(timeoutSec * 3L);
            if (node.getLastHeartbeat().isBefore(deadline)) {
                node.setStatus("OFFLINE");
                clientMapper.updateById(node);
                taskRuntimeService.markNodeTasksFailed(node.getNodeId(), "爬虫节点离线超时，任务终止");
            }
        }
    }
}
