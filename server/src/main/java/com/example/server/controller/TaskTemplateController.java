package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.TaskTemplate;
import com.example.server.service.TaskTemplateService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/templates")
public class TaskTemplateController {
    private final TaskTemplateService taskTemplateService;

    public TaskTemplateController(TaskTemplateService taskTemplateService) {
        this.taskTemplateService = taskTemplateService;
    }

    @GetMapping
    public ApiResponse<List<TaskTemplate>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(taskTemplateService.listByUser(userId));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody TaskTemplate template) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (template.getTemplateName() == null || template.getTemplateName().isBlank()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "模板名称不能为空");
        }
        template.setUserId(userId);
        return ApiResponse.success(taskTemplateService.create(template));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody TaskTemplate template) {
        Long userId = SecurityUtils.getCurrentUserId();
        template.setTemplateId(id);
        TaskTemplate updated = taskTemplateService.update(template, userId);
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "模板不存在或无权修改");
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!taskTemplateService.delete(id, userId)) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "模板不存在或无权删除");
        }
        return ApiResponse.success(null);
    }
}
