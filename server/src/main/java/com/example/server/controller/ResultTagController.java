package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.ResultTag;
import com.example.server.entity.ResultTagView;
import com.example.server.service.ResultTagService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/result-tags")
public class ResultTagController {
    private final ResultTagService resultTagService;

    public ResultTagController(ResultTagService resultTagService) {
        this.resultTagService = resultTagService;
    }

    @GetMapping
    public ApiResponse<List<ResultTagView>> list() {
        return ApiResponse.success(resultTagService.list(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody ResultTag tag) {
        ResultTag created = resultTagService.create(tag, SecurityUtils.getCurrentUserId());
        if (created == null) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "标签名称不能为空");
        }
        return ApiResponse.success(created);
    }

    @PutMapping("/{tagId}")
    public ApiResponse<?> update(@PathVariable Long tagId, @RequestBody ResultTag tag) {
        tag.setTagId(tagId);
        ResultTag updated = resultTagService.update(tag, SecurityUtils.getCurrentUserId());
        if (updated == null) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "标签不存在或无权限访问");
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{tagId}")
    public ApiResponse<?> delete(@PathVariable Long tagId) {
        if (!resultTagService.delete(tagId, SecurityUtils.getCurrentUserId())) {
            return ApiResponse.error(ErrorCode.NOT_FOUND, "标签不存在或无权限访问");
        }
        return ApiResponse.success(true);
    }

    @PostMapping("/{tagId}/bindings")
    public ApiResponse<?> bind(@PathVariable Long tagId, @RequestParam Long pageResultId) {
        if (!resultTagService.bind(tagId, pageResultId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限绑定该结果标签");
        }
        return ApiResponse.success(true);
    }

    @DeleteMapping("/{tagId}/bindings")
    public ApiResponse<?> unbind(@PathVariable Long tagId, @RequestParam Long pageResultId) {
        return ApiResponse.success(resultTagService.unbind(tagId, pageResultId, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/bindings")
    public ApiResponse<Map<Long, List<ResultTagView>>> queryBindings(@RequestParam List<Long> pageResultIds) {
        return ApiResponse.success(resultTagService.queryPageResultTags(SecurityUtils.getCurrentUserId(), pageResultIds));
    }
}
