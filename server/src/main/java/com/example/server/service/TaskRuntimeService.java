package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Task;
import com.example.server.entity.TaskRuntime;
import com.example.server.mapper.CrawlerPageResultMapper;
import com.example.server.mapper.TaskMapper;
import com.example.server.mapper.TaskRuntimeMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskRuntimeService {
    private final TaskRuntimeMapper taskRuntimeMapper;
    private final TaskMapper taskMapper;
    private final CrawlerPageResultMapper crawlerPageResultMapper;
    private final TaskRuntimeStreamService taskRuntimeStreamService;

    public TaskRuntimeService(
            TaskRuntimeMapper taskRuntimeMapper,
            TaskMapper taskMapper,
            CrawlerPageResultMapper crawlerPageResultMapper,
            TaskRuntimeStreamService taskRuntimeStreamService) {
        this.taskRuntimeMapper = taskRuntimeMapper;
        this.taskMapper = taskMapper;
        this.crawlerPageResultMapper = crawlerPageResultMapper;
        this.taskRuntimeStreamService = taskRuntimeStreamService;
    }

    public void createQueuedRuntime(Task task) {
        if (task == null || task.getTaskId() == null) {
            return;
        }

        TaskRuntime runtime = new TaskRuntime();
        runtime.setTaskId(task.getTaskId());
        runtime.setStatus("PENDING");
        runtime.setAssignedNodeId(task.getNodeId());
        runtime.setProgressPercent(0);
        runtime.setExpectedPages(task.getTotalPages() == null ? 0 : task.getTotalPages());
        runtime.setCompletedPages(0);
        runtime.setSuccessPages(0);
        runtime.setFailedPages(0);
        runtime.setRetryCount(task.getRetryCount() == null ? 0 : task.getRetryCount());
        runtime.setLastErrorMessage(task.getLastErrorMessage());
        runtime.setQueuedAt(LocalDateTime.now());
        taskRuntimeMapper.insert(runtime);
        taskRuntimeStreamService.publish(runtime);
    }

    public TaskRuntime getByTaskId(Long taskId) {
        return taskId == null ? null : taskRuntimeMapper.selectById(taskId);
    }

    public void save(TaskRuntime runtime) {
        if (runtime == null || runtime.getTaskId() == null) {
            return;
        }
        TaskRuntime existing = taskRuntimeMapper.selectById(runtime.getTaskId());
        if (existing == null) {
            taskRuntimeMapper.insert(runtime);
            taskRuntimeStreamService.publish(runtime);
            return;
        }
        taskRuntimeMapper.updateById(runtime);
        taskRuntimeStreamService.publish(runtime);
    }

    public void markStarted(Long taskId, String nodeId) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }
        if (isTerminalStatus(runtime.getStatus())) {
            return;
        }
        runtime.setStatus("RUNNING");
        runtime.setAssignedNodeId(nodeId);
        runtime.setStartedAt(runtime.getStartedAt() == null ? LocalDateTime.now() : runtime.getStartedAt());
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
        taskRuntimeStreamService.publish(runtime);
    }

    public void markProgress(Long taskId, String nodeId, Integer totalPages, boolean success, String errorCode, String errorMessage) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }

        runtime.setAssignedNodeId(nodeId);
        if (runtime.getStartedAt() == null) {
            runtime.setStartedAt(LocalDateTime.now());
        }
        if (totalPages != null && totalPages > 0) {
            runtime.setExpectedPages(totalPages);
        }
        runtime.setCompletedPages(safe(runtime.getCompletedPages()) + 1);
        if (success) {
            runtime.setSuccessPages(safe(runtime.getSuccessPages()) + 1);
        } else {
            runtime.setFailedPages(safe(runtime.getFailedPages()) + 1);
            runtime.setLastErrorCode(errorCode);
            runtime.setLastErrorMessage(errorMessage);
        }

        int expected = Math.max(safe(runtime.getExpectedPages()), runtime.getCompletedPages());
        boolean terminal = isTerminalStatus(runtime.getStatus());
        runtime.setProgressPercent(expected <= 0 ? 0 : Math.min(100, Math.round(runtime.getCompletedPages() * 100f / expected)));
        if (terminal) {
            runtime.setProgressPercent(100);
        } else if (expected > 0 && runtime.getCompletedPages() >= expected) {
            runtime.setStatus(resolveFinalStatus(runtime));
            runtime.setProgressPercent(100);
            if (runtime.getFinishedAt() == null) {
                runtime.setFinishedAt(LocalDateTime.now());
            }
        } else {
            runtime.setStatus("RUNNING");
        }
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
        taskRuntimeStreamService.publish(runtime);
    }

    public void syncWithPageResults(
            Long taskId,
            String nodeId,
            Integer totalPages,
            boolean latestSuccess,
            String errorCode,
            String errorMessage) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }

        runtime.setAssignedNodeId(nodeId);
        if (runtime.getStartedAt() == null) {
            runtime.setStartedAt(LocalDateTime.now());
        }
        if (totalPages != null && totalPages > 0) {
            runtime.setExpectedPages(totalPages);
        }

        List<CrawlerPageResultRecord> results = crawlerPageResultMapper.selectList(new LambdaQueryWrapper<CrawlerPageResultRecord>()
                .eq(CrawlerPageResultRecord::getTaskId, taskId));
        int completedPages = results.size();
        int successPages = (int) results.stream().filter(result -> Boolean.TRUE.equals(result.getSuccess())).count();
        int failedPages = completedPages - successPages;

        runtime.setCompletedPages(completedPages);
        runtime.setSuccessPages(successPages);
        runtime.setFailedPages(failedPages);

        if (!latestSuccess) {
            runtime.setLastErrorCode(errorCode);
            runtime.setLastErrorMessage(errorMessage);
        }

        int expected = Math.max(safe(runtime.getExpectedPages()), completedPages);
        boolean terminal = isTerminalStatus(runtime.getStatus());
        runtime.setProgressPercent(expected <= 0 ? 0 : Math.min(100, Math.round(completedPages * 100f / expected)));
        if (terminal) {
            runtime.setProgressPercent(100);
        } else if (completedPages > 0 && expected > 0 && completedPages >= expected) {
            runtime.setStatus(resolveFinalStatus(runtime));
            runtime.setProgressPercent(100);
            if (runtime.getFinishedAt() == null) {
                runtime.setFinishedAt(LocalDateTime.now());
            }
        } else if (completedPages > 0) {
            runtime.setStatus("RUNNING");
        }
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
        taskRuntimeStreamService.publish(runtime);
    }

    public void recountFromPageResults(Long taskId) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }
        syncWithPageResults(
                taskId,
                runtime.getAssignedNodeId(),
                runtime.getExpectedPages(),
                safe(runtime.getFailedPages()) == 0,
                runtime.getLastErrorCode(),
                runtime.getLastErrorMessage());
    }

    public void markFinished(Long taskId, String nodeId, boolean success, Integer totalPages, Integer successPages, Integer failedPages, String errorCode, String errorMessage) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }
        runtime.setAssignedNodeId(nodeId);
        if (totalPages != null && totalPages > 0) {
            runtime.setExpectedPages(totalPages);
        }
        if (successPages != null) {
            runtime.setSuccessPages(Math.max(safe(runtime.getSuccessPages()), successPages));
        }
        if (failedPages != null) {
            runtime.setFailedPages(Math.max(safe(runtime.getFailedPages()), failedPages));
        }
        runtime.setCompletedPages(Math.max(safe(runtime.getCompletedPages()), safe(runtime.getSuccessPages()) + safe(runtime.getFailedPages())));
        if (errorCode != null && !errorCode.isBlank()) {
            runtime.setLastErrorCode(errorCode);
        }
        runtime.setStatus(resolveFinalStatus(runtime));
        runtime.setProgressPercent(100);
        runtime.setFinishedAt(LocalDateTime.now());
        if (safe(runtime.getFailedPages()) > 0 || !success) {
            runtime.setLastErrorMessage(errorMessage);
        }
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
        taskRuntimeStreamService.publish(runtime);
    }

    public void markPendingTimeout(LocalDateTime cutoff) {
        taskRuntimeMapper.selectList(new LambdaQueryWrapper<TaskRuntime>()
                        .eq(TaskRuntime::getStatus, "PENDING")
                        .lt(TaskRuntime::getQueuedAt, cutoff))
                .forEach(runtime -> {
                    runtime.setStatus("FAILED");
                    runtime.setProgressPercent(100);
                    runtime.setLastErrorMessage("任务排队超时");
                    runtime.setFinishedAt(LocalDateTime.now());
                    runtime.setUpdatedAt(LocalDateTime.now());
                    taskRuntimeMapper.updateById(runtime);
                    syncTaskSnapshot(runtime.getTaskId(), runtime);
                    taskRuntimeStreamService.publish(runtime);
                });
    }

    private TaskRuntime ensureRuntime(Long taskId) {
        if (taskId == null) {
            return null;
        }
        TaskRuntime runtime = taskRuntimeMapper.selectById(taskId);
        if (runtime != null) {
            return runtime;
        }

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }

        runtime = new TaskRuntime();
        runtime.setTaskId(taskId);
        runtime.setStatus(task.getTaskStatus() == null ? "PENDING" : task.getTaskStatus());
        runtime.setAssignedNodeId(task.getNodeId());
        runtime.setProgressPercent(task.getTaskProgress() == null ? 0 : task.getTaskProgress());
        runtime.setExpectedPages(task.getTotalPages());
        runtime.setCompletedPages(0);
        runtime.setSuccessPages(0);
        runtime.setFailedPages(0);
        runtime.setRetryCount(task.getRetryCount() == null ? 0 : task.getRetryCount());
        runtime.setLastErrorMessage(task.getLastErrorMessage());
        runtime.setQueuedAt(task.getCreatedAt());
        runtime.setStartedAt(task.getStartedAt());
        runtime.setFinishedAt(task.getFinishedAt());
        taskRuntimeMapper.insert(runtime);
        return runtime;
    }

    private void syncTaskSnapshot(Long taskId, TaskRuntime runtime) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setNodeId(runtime.getAssignedNodeId());
        task.setTaskStatus(runtime.getStatus());
        task.setTaskProgress(runtime.getProgressPercent());
        task.setTotalPages(runtime.getExpectedPages());
        task.setRetryCount(runtime.getRetryCount());
        task.setLastErrorMessage(runtime.getLastErrorMessage());
        task.setStartedAt(runtime.getStartedAt());
        task.setFinishedAt(runtime.getFinishedAt());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isTerminalStatus(String status) {
        if (status == null) {
            return false;
        }
        return "FINISHED".equalsIgnoreCase(status)
                || "FAILED".equalsIgnoreCase(status)
                || "PARTIAL_FAILED".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status);
    }

    private String resolveFinalStatus(TaskRuntime runtime) {
        int successCount = safe(runtime.getSuccessPages());
        int failedCount = safe(runtime.getFailedPages());
        if (failedCount > 0 && successCount > 0) {
            return "PARTIAL_FAILED";
        }
        if (successCount > 0) {
            return "FINISHED";
        }
        if (failedCount > 0) {
            return "FAILED";
        }
        return safe(runtime.getCompletedPages()) > 0 ? "FINISHED" : "PENDING";
    }
}
