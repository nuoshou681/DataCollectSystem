package com.example.server.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.Crawler;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.mapper.ClientMapper;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class CrawlerController {

    @Autowired
    private ClientMapper clientmapper;

    @GetMapping("/client")
    public ApiResponse<List<Crawler>> getMethodName() {
        LambdaQueryWrapper<Crawler> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Crawler::getLastHeartbeat);
        return ApiResponse.success(clientmapper.selectList(queryWrapper));
    }

}
