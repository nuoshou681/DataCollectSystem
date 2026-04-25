package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.service.ResultBookmarkService;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/bookmarks")
public class ResultBookmarkController {
    private final ResultBookmarkService resultBookmarkService;

    public ResultBookmarkController(ResultBookmarkService resultBookmarkService) {
        this.resultBookmarkService = resultBookmarkService;
    }

    @GetMapping
    public ApiResponse<List<CrawlerPageResultRecord>> list() {
        return ApiResponse.success(resultBookmarkService.listBookmarks(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/ids")
    public ApiResponse<Set<Long>> ids() {
        return ApiResponse.success(resultBookmarkService.queryBookmarkedPageResultIds(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestParam Long pageResultId) {
        if (!resultBookmarkService.addBookmark(SecurityUtils.getCurrentUserId(), pageResultId, SecurityUtils.isAdmin())) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "无权限收藏该结果");
        }
        return ApiResponse.success(true);
    }

    @DeleteMapping
    public ApiResponse<?> delete(@RequestParam Long pageResultId) {
        return ApiResponse.success(resultBookmarkService.removeBookmark(SecurityUtils.getCurrentUserId(), pageResultId));
    }
}
