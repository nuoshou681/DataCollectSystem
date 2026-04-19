package com.example.server.controller;

import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.SubTask;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.CrawlerPageResult;
import com.example.server.mapper.SubTaskMapper;
import com.example.server.service.CrawlerRuntimeStore;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class SubTaskController {

    @Autowired
    private SubTaskMapper subTaskMapper;

    @Autowired
    private CrawlerRuntimeStore crawlerRuntimeStore;

    @GetMapping("/task/sub")
    public ApiResponse<List<SubTask>> queryList(@RequestParam(required = false) Long taskId) {
        LambdaQueryWrapper<SubTask> queryWrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            queryWrapper.eq(SubTask::getTaskId, taskId);
        }
        queryWrapper.orderByDesc(SubTask::getSubtaskId);
        return ApiResponse.success(subTaskMapper.selectList(queryWrapper));
    }

    @GetMapping("/task/page-results")
    public ApiResponse<List<CrawlerPageResult>> pageResults(@RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long subTaskId) {
        if (subTaskId != null) {
            return ApiResponse.success(crawlerRuntimeStore.getBySubTaskId(subTaskId));
        }
        if (taskId != null) {
            return ApiResponse.success(crawlerRuntimeStore.getByTaskId(taskId));
        }
        return ApiResponse.success(crawlerRuntimeStore.getAll());
    }

}
