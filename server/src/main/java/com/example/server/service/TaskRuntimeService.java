package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.Task;
import com.example.server.entity.TaskRuntime;
import com.example.server.mapper.TaskMapper;
import com.example.server.mapper.TaskRuntimeMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class TaskRuntimeService {
    private final TaskRuntimeMapper taskRuntimeMapper;
    private final TaskMapper taskMapper;

    public TaskRuntimeService(TaskRuntimeMapper taskRuntimeMapper, TaskMapper taskMapper) {
        this.taskRuntimeMapper = taskRuntimeMapper;
        this.taskMapper = taskMapper;
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
            return;
        }
        taskRuntimeMapper.updateById(runtime);
    }

    public void markStarted(Long taskId, String nodeId) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }
        runtime.setStatus("RUNNING");
        runtime.setAssignedNodeId(nodeId);
        runtime.setStartedAt(runtime.getStartedAt() == null ? LocalDateTime.now() : runtime.getStartedAt());
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
    }

    public void markProgress(Long taskId, String nodeId, Integer totalPages, boolean success, String errorCode, String errorMessage) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }

        runtime.setStatus("RUNNING");
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
        runtime.setProgressPercent(expected <= 0 ? 0 : Math.min(100, Math.round(runtime.getCompletedPages() * 100f / expected)));
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
    }

    public void markFinished(Long taskId, String nodeId, boolean success, Integer totalPages, String errorMessage) {
        TaskRuntime runtime = ensureRuntime(taskId);
        if (runtime == null) {
            return;
        }
        runtime.setAssignedNodeId(nodeId);
        if (totalPages != null && totalPages > 0) {
            runtime.setExpectedPages(totalPages);
        }
        runtime.setStatus(success ? (safe(runtime.getFailedPages()) > 0 ? "PARTIAL_FAILED" : "FINISHED") : "FAILED");
        runtime.setProgressPercent(100);
        runtime.setFinishedAt(LocalDateTime.now());
        if (!success) {
            runtime.setLastErrorMessage(errorMessage);
        }
        runtime.setUpdatedAt(LocalDateTime.now());
        taskRuntimeMapper.updateById(runtime);
        syncTaskSnapshot(taskId, runtime);
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
}
