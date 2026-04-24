package com.example.server.service;

import java.util.List;

import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.Task;

public interface TaskService {
    // 拆分请求并派发任务
    public List<Task> dispatchTasks(DispatchTaskRequest request);

    public List<Task> buildTasks(DispatchTaskRequest request);
}
