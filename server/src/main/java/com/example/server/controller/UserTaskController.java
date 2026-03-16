package com.example.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.UserTask;
import com.example.server.mapper.UserTaskMapper;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class UserTaskController {
    @Autowired
    private UserTaskMapper userTaskMapper;

    @GetMapping("/task")
    public List<UserTask> queryList() {
        return userTaskMapper.selectList(null);
    }
    
}
