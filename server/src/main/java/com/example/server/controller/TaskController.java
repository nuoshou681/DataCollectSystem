package com.example.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.TaskService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/task")
public class TaskController {
    @Autowired
    private TaskMapper userTaskMapper;

    @Autowired
    private TaskService taskService;

    @GetMapping("/task")
    public List<Task> queryList() {
        return userTaskMapper.selectList(null);
    }

    @PostMapping("/dispatch") // 实际路径: /task/dispatch
    public String dispatchTask(@RequestBody Task task) {
        taskService.splitAndDispatchSubTasks(task);
        return "success";
    }

}
