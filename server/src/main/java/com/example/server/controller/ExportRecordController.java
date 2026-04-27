package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.ExportRecord;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.service.ExportRecordService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/exports")
public class ExportRecordController {
    private final ExportRecordService exportRecordService;

    public ExportRecordController(ExportRecordService exportRecordService) {
        this.exportRecordService = exportRecordService;
    }

    @GetMapping
    public ApiResponse<List<ExportRecord>> list() {
        return ApiResponse.success(exportRecordService.list(SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }
}
