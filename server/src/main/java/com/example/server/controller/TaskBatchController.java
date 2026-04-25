package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.TaskBatch;
import com.example.server.entity.TaskBatchDetailView;
import com.example.server.service.TaskBatchService;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/batches")
public class TaskBatchController {
    private final TaskBatchService taskBatchService;

    public TaskBatchController(TaskBatchService taskBatchService) {
        this.taskBatchService = taskBatchService;
    }

    @GetMapping
    public ApiResponse<List<TaskBatch>> list() {
        return ApiResponse.success(taskBatchService.list(SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<?> detail(@PathVariable String batchId) {
        TaskBatchDetailView detail = taskBatchService.detail(batchId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
        if (detail == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "批次不存在或无权限访问");
        }
        return ApiResponse.success(detail);
    }
}
