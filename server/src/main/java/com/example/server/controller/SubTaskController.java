package com.example.server.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.SubTask;
import com.example.server.mapper.SubTaskMapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class SubTaskController {

    @Autowired
    private SubTaskMapper subTaskMapper;

    @GetMapping("/task/sub")
    public List<SubTask> queryList() {
        return subTaskMapper.selectList(null);
    }
    
}
