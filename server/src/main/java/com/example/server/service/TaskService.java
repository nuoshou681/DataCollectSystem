package com.example.server.service;

import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.Task;
import com.example.server.entity.TaskDetailView;
import java.util.List;

public interface TaskService {
    List<Task> dispatchTasks(DispatchTaskRequest request);

    List<Task> queryTasks(Long userId, boolean isAdmin);

    TaskDetailView queryTaskDetail(Long taskId, Long userId, boolean isAdmin);

    List<Task> dispatchBatchTasks(DispatchTaskRequest request);

    boolean updateArchived(Long taskId, boolean archived, Long userId, boolean isAdmin);

    boolean retryTask(Long taskId, Long userId, boolean isAdmin);

    int batchArchive(List<Long> taskIds, boolean archived, Long userId, boolean isAdmin);

    int batchRetry(List<Long> taskIds, Long userId, boolean isAdmin);
}
