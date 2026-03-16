package com.example.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.TaskLog;
import com.example.server.mapper.TaskLogMapper;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class TaskLogController {
    @Autowired
    private TaskLogMapper taskLogMapper;

    @GetMapping("/log")
    public List<TaskLog> getMethodName() {
        return taskLogMapper.selectList(null);
    }
    
}
