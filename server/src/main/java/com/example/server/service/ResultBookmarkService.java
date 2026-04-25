package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.ResultBookmark;
import com.example.server.mapper.CrawlerPageResultMapper;
import com.example.server.mapper.ResultBookmarkMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResultBookmarkService {
    private final ResultBookmarkMapper resultBookmarkMapper;
    private final CrawlerPageResultMapper crawlerPageResultMapper;
    private final CrawlerPageResultService crawlerPageResultService;

    public ResultBookmarkService(
            ResultBookmarkMapper resultBookmarkMapper,
            CrawlerPageResultMapper crawlerPageResultMapper,
            CrawlerPageResultService crawlerPageResultService) {
        this.resultBookmarkMapper = resultBookmarkMapper;
        this.crawlerPageResultMapper = crawlerPageResultMapper;
        this.crawlerPageResultService = crawlerPageResultService;
    }

    public boolean addBookmark(Long userId, Long pageResultId, boolean isAdmin) {
        if (userId == null || pageResultId == null || !crawlerPageResultService.canAccessPageResult(pageResultId, userId, isAdmin)) {
            return false;
        }
        ResultBookmark existing = resultBookmarkMapper.selectOne(new LambdaQueryWrapper<ResultBookmark>()
                .eq(ResultBookmark::getUserId, userId)
                .eq(ResultBookmark::getPageResultId, pageResultId));
        if (existing != null) {
            return true;
        }
        ResultBookmark bookmark = new ResultBookmark();
        bookmark.setUserId(userId);
        bookmark.setPageResultId(pageResultId);
        bookmark.setCreatedAt(LocalDateTime.now());
        resultBookmarkMapper.insert(bookmark);
        return true;
    }

    public boolean removeBookmark(Long userId, Long pageResultId) {
        if (userId == null || pageResultId == null) {
            return false;
        }
        return resultBookmarkMapper.delete(new LambdaQueryWrapper<ResultBookmark>()
                .eq(ResultBookmark::getUserId, userId)
                .eq(ResultBookmark::getPageResultId, pageResultId)) > 0;
    }

    public Set<Long> queryBookmarkedPageResultIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return resultBookmarkMapper.selectList(new LambdaQueryWrapper<ResultBookmark>()
                        .eq(ResultBookmark::getUserId, userId))
                .stream()
                .map(ResultBookmark::getPageResultId)
                .collect(Collectors.toSet());
    }

    public List<CrawlerPageResultRecord> listBookmarks(Long userId) {
        Set<Long> ids = queryBookmarkedPageResultIds(userId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return crawlerPageResultMapper.selectList(new LambdaQueryWrapper<CrawlerPageResultRecord>()
                .in(CrawlerPageResultRecord::getPageResultId, ids)
                .orderByDesc(CrawlerPageResultRecord::getCreatedAt));
    }
}
