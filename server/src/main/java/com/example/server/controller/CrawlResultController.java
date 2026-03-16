package com.example.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.CrawlResult;
import com.example.server.mapper.CrawlResultMapper;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class CrawlResultController {
    @Autowired
    private CrawlResultMapper crawlResultMapper;

    @GetMapping("/crawlresult")
    public List<CrawlResult> getMethodName() {
        return crawlResultMapper.selectList(null);
    }
    
}
