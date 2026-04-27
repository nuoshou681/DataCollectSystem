package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.Task;
import com.example.server.entity.TaskDetailView;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.service.TaskService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<List<Task>> queryList() {
        return ApiResponse.success(taskService.queryTasks(SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<?> detail(@PathVariable Long taskId) {
        TaskDetailView detail = taskService.queryTaskDetail(taskId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
        if (detail == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "任务不存在或无权限访问");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping("/dispatch")
    public ApiResponse<?> dispatchTask(@RequestBody DispatchTaskRequest request) {
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()
                || request.getUrl() == null || request.getUrl().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "关键词和网站地址不能为空");
        }

        try {
            request.setUserId(SecurityUtils.getCurrentUserId());
            List<Task> tasks = taskService.dispatchTasks(request);
            return ApiResponse.success(tasks);
        } catch (Exception e) {
            log.error("任务分发失败, keyword={}, url={}", request.getKeyword(), request.getUrl(), e);
            return ApiResponse.error(ErrorCode.SERVER_ERROR, "任务分发失败: " + e.getMessage());
        }
    }

    @PostMapping("/dispatch-batch")
    public ApiResponse<?> dispatchBatchTask(@RequestBody DispatchTaskRequest request) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()
                || request.getKeywordsText() == null || request.getKeywordsText().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "种子链接和批量关键词不能为空");
        }

        try {
            request.setUserId(SecurityUtils.getCurrentUserId());
            List<Task> tasks = taskService.dispatchBatchTasks(request);
            return ApiResponse.success(tasks);
        } catch (Exception e) {
            log.error("批量任务分发失败, url={}", request.getUrl(), e);
            return ApiResponse.error(ErrorCode.SERVER_ERROR, "批量任务分发失败: " + e.getMessage());
        }
    }

    @PostMapping("/{taskId}/archive")
    public ApiResponse<?> updateArchive(@PathVariable Long taskId, @RequestParam boolean archived) {
        boolean updated = taskService.updateArchived(taskId, archived, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
        if (!updated) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "任务不存在或无权限归档");
        }
        return ApiResponse.success(true);
    }
}
