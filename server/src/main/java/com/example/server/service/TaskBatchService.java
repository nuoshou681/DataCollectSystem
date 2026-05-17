package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.Task;
import com.example.server.entity.TaskBatch;
import com.example.server.entity.TaskBatchDetailView;
import com.example.server.entity.TaskRuntime;
import com.example.server.mapper.TaskBatchMapper;
import com.example.server.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskBatchService {
    private final TaskBatchMapper taskBatchMapper;
    private final TaskMapper taskMapper;
    private final TaskRuntimeService taskRuntimeService;

    public TaskBatchService(TaskBatchMapper taskBatchMapper, TaskMapper taskMapper, TaskRuntimeService taskRuntimeService) {
        this.taskBatchMapper = taskBatchMapper;
        this.taskMapper = taskMapper;
        this.taskRuntimeService = taskRuntimeService;
    }

    public void createBatchIfAbsent(String batchId, String batchName, Long userId, int taskCount, String notes) {
        if (batchId == null || batchId.isBlank()) {
            return;
        }
        TaskBatch existing = taskBatchMapper.selectById(batchId);
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        TaskBatch batch = new TaskBatch();
        batch.setBatchId(batchId);
        batch.setBatchName(batchName == null || batchName.isBlank() ? batchId : batchName);
        batch.setCreatedBy(userId);
        batch.setTaskCount(taskCount);
        batch.setStatus("PENDING");
        batch.setNotes(notes);
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        taskBatchMapper.insert(batch);
    }

    public List<TaskBatch> list(Long userId, boolean isAdmin) {
        LambdaQueryWrapper<TaskBatch> query = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            query.eq(TaskBatch::getCreatedBy, userId);
        }
        query.orderByDesc(TaskBatch::getCreatedAt);
        return taskBatchMapper.selectList(query);
    }

    public void syncBatchStatus(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return;
        }
        TaskBatch batch = taskBatchMapper.selectById(batchId);
        if (batch == null) {
            return;
        }
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>().eq(Task::getBatchId, batchId));
        int count = tasks.size();
        long pending = tasks.stream().filter(task -> "PENDING".equalsIgnoreCase(task.getTaskStatus())).count();
        long running = tasks.stream().filter(task -> "RUNNING".equalsIgnoreCase(task.getTaskStatus())).count();
        long failed = tasks.stream().filter(task -> "FAILED".equalsIgnoreCase(task.getTaskStatus()) || "PARTIAL_FAILED".equalsIgnoreCase(task.getTaskStatus())).count();

        String status = "PENDING";
        if (count == 0) {
            status = "EMPTY";
        } else if (running > 0) {
            status = "RUNNING";
        } else if (pending == count) {
            status = "PENDING";
        } else if (failed > 0) {
            status = "PARTIAL_FAILED";
        } else {
            status = "FINISHED";
        }

        batch.setTaskCount(count);
        batch.setStatus(status);
        batch.setUpdatedAt(LocalDateTime.now());
        taskBatchMapper.updateById(batch);
    }

    public TaskBatchDetailView detail(String batchId, Long userId, boolean isAdmin) {
        if (batchId == null || batchId.isBlank()) {
            return null;
        }
        TaskBatch batch = taskBatchMapper.selectById(batchId);
        if (batch == null) {
            // Auto-create missing batch record when tasks reference this batchId
            List<Task> existingTasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                    .eq(Task::getBatchId, batchId));
            if (existingTasks.isEmpty()) {
                return null;
            }
            Long createdBy = existingTasks.stream()
                    .map(Task::getUserId)
                    .filter(id -> id != null)
                    .findFirst().orElse(null);
            LocalDateTime now = LocalDateTime.now();
            batch = new TaskBatch();
            batch.setBatchId(batchId);
            batch.setBatchName(batchId);
            batch.setCreatedBy(createdBy);
            batch.setTaskCount(existingTasks.size());
            batch.setStatus("PENDING");
            batch.setNotes("(自动修复)");
            batch.setCreatedAt(now);
            batch.setUpdatedAt(now);
            taskBatchMapper.insert(batch);
        }
        if (!isAdmin && (userId == null || !userId.equals(batch.getCreatedBy()))) {
            return null;
        }
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getBatchId, batchId)
                .orderByDesc(Task::getTaskId));
        for (Task task : tasks) {
            TaskRuntime runtime = taskRuntimeService.getByTaskId(task.getTaskId());
            task.setRuntime(runtime);
        }
        return new TaskBatchDetailView(batch, tasks);
    }
}
