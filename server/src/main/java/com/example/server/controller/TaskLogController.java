package com.example.server.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.TaskLog;
import com.example.server.mapper.TaskLogMapper;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class TaskLogController {
    @Autowired
    private TaskLogMapper taskLogMapper;

    @GetMapping("/log")
    public ApiResponse<List<TaskLog>> getMethodName() {
        LambdaQueryWrapper<TaskLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(TaskLog::getLogId);
        return ApiResponse.success(taskLogMapper.selectList(queryWrapper));
    }

}
