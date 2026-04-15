package com.example.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.SubTask;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.TaskDispatchService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class UserTaskController {
    @Autowired
    private TaskMapper userTaskMapper;

    @Autowired
    private TaskDispatchService taskDispatchService;

    @GetMapping("/task")
    public List<Task> queryList() {
        return userTaskMapper.selectList(null);
    }

    @PostMapping("/dispatch")
    public String postMethodName(@RequestBody SubTask subTask) {
        taskDispatchService.dispatchTask(subTask);
        return "success";
    }

}
