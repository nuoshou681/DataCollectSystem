package com.example.server.service;

import java.util.List;

import com.example.server.entity.SubTask;

public interface SubTaskService {
    public void dispatchSubTasks(List<SubTask>subTasks);
}
