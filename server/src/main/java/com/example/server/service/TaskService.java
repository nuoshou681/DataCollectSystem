package com.example.server.service;

import java.util.List;

import com.example.server.entity.SubTask;
import com.example.server.entity.Task;

public interface TaskService {
    // 拆分用户任务为子任务
    public void splitAndDispatchSubTasks(Task task);

    public List<SubTask> buildSubTasks(Task task);
}
