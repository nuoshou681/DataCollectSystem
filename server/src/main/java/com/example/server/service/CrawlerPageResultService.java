package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Task;
import com.example.server.entity.Message.CrawlerPageResult;
import com.example.server.mapper.CrawlerPageResultMapper;
import com.example.server.mapper.TaskMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CrawlerPageResultService {

    private final CrawlerPageResultMapper crawlerPageResultMapper;
    private final TaskMapper taskMapper;

    public CrawlerPageResultService(CrawlerPageResultMapper crawlerPageResultMapper, TaskMapper taskMapper) {
        this.crawlerPageResultMapper = crawlerPageResultMapper;
        this.taskMapper = taskMapper;
    }

    public CrawlerPageResultRecord saveOrUpdateFromMessage(CrawlerPageResult message) {
        if (message == null || message.getTaskId() == null
                || message.getPageUrl() == null
                || message.getPageUrl().isBlank()) {
            return null;
        }

        LambdaQueryWrapper<CrawlerPageResultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CrawlerPageResultRecord::getTaskId, message.getTaskId())
                .eq(CrawlerPageResultRecord::getPageIndex, message.getPageIndex())
                .eq(CrawlerPageResultRecord::getPageUrl, message.getPageUrl());

        CrawlerPageResultRecord exists = crawlerPageResultMapper.selectOne(queryWrapper);

        if (exists == null) {
            CrawlerPageResultRecord insert = new CrawlerPageResultRecord();
            insert.setTaskId(message.getTaskId());
            insert.setNodeId(message.getNodeId());
            insert.setPageUrl(message.getPageUrl());
            insert.setPageTitle(message.getPageTitle());
            insert.setPageIndex(message.getPageIndex());
            insert.setSuccess(message.isSuccess());
            insert.setFilePath(message.getFilePath());
            insert.setErrorMessage(message.getErrorMessage());
            insert.setMhtmlCached(false);
            crawlerPageResultMapper.insert(insert);
            updateTaskNodeIdIfMissing(message.getTaskId(), message.getNodeId());
            return crawlerPageResultMapper.selectById(insert.getPageResultId());
        }

        LambdaUpdateWrapper<CrawlerPageResultRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CrawlerPageResultRecord::getPageResultId, exists.getPageResultId())
                .set(CrawlerPageResultRecord::getTaskId, message.getTaskId())
                .set(CrawlerPageResultRecord::getNodeId, message.getNodeId())
                .set(CrawlerPageResultRecord::getPageTitle, message.getPageTitle())
                .set(CrawlerPageResultRecord::getSuccess, message.isSuccess())
                .set(CrawlerPageResultRecord::getFilePath, message.getFilePath())
                .set(CrawlerPageResultRecord::getErrorMessage, message.getErrorMessage());

        crawlerPageResultMapper.update(null, updateWrapper);
        updateTaskNodeIdIfMissing(message.getTaskId(), message.getNodeId());
        return crawlerPageResultMapper.selectById(exists.getPageResultId());
    }

    public List<CrawlerPageResultRecord> queryResults(Long taskId) {
        LambdaQueryWrapper<CrawlerPageResultRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            queryWrapper.eq(CrawlerPageResultRecord::getTaskId, taskId);
        }
        queryWrapper.orderByAsc(CrawlerPageResultRecord::getTaskId)
                .orderByAsc(CrawlerPageResultRecord::getPageIndex)
                .orderByAsc(CrawlerPageResultRecord::getPageResultId);
        return crawlerPageResultMapper.selectList(queryWrapper);
    }

    public CrawlerPageResultRecord cacheMhtml(Long pageResultId) throws IOException {
        CrawlerPageResultRecord pageResult = crawlerPageResultMapper.selectById(pageResultId);
        if (pageResult == null) {
            return null;
        }
        if (pageResult.getFilePath() == null || pageResult.getFilePath().isBlank()) {
            throw new IOException("MHTML 路径为空，无法缓存");
        }

        Path mhtmlPath = Path.of(pageResult.getFilePath());
        if (!Files.exists(mhtmlPath)) {
            throw new IOException("MHTML 文件不存在: " + pageResult.getFilePath());
        }

        String content = Files.readString(mhtmlPath, StandardCharsets.UTF_8);

        LambdaUpdateWrapper<CrawlerPageResultRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CrawlerPageResultRecord::getPageResultId, pageResultId)
                .set(CrawlerPageResultRecord::getMhtmlContent, content)
                .set(CrawlerPageResultRecord::getMhtmlCached, true)
                .set(CrawlerPageResultRecord::getMhtmlCachedAt, LocalDateTime.now());
        crawlerPageResultMapper.update(null, updateWrapper);

        return crawlerPageResultMapper.selectById(pageResultId);
    }

    public MhtmlDownloadData loadMhtmlForDownload(Long pageResultId) throws IOException {
        CrawlerPageResultRecord pageResult = queryByIdForDownload(pageResultId);
        if (pageResult == null) {
            return null;
        }

        byte[] content = null;
        if (pageResult.getFilePath() != null && !pageResult.getFilePath().isBlank()) {
            Path mhtmlPath = Path.of(pageResult.getFilePath());
            if (Files.exists(mhtmlPath)) {
                content = Files.readAllBytes(mhtmlPath);
            }
        }

        if (content == null && pageResult.getMhtmlContent() != null && !pageResult.getMhtmlContent().isBlank()) {
            content = pageResult.getMhtmlContent().getBytes(StandardCharsets.UTF_8);
        }

        if (content == null) {
            throw new IOException("MHTML 文件不存在且数据库中无缓存内容");
        }

        return new MhtmlDownloadData(buildDownloadFileName(pageResult), content);
    }

    private CrawlerPageResultRecord queryByIdForDownload(Long pageResultId) {
        LambdaQueryWrapper<CrawlerPageResultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CrawlerPageResultRecord::getPageResultId, pageResultId)
                .select(
                        CrawlerPageResultRecord::getPageResultId,
                        CrawlerPageResultRecord::getTaskId,
                        CrawlerPageResultRecord::getPageIndex,
                        CrawlerPageResultRecord::getFilePath,
                        CrawlerPageResultRecord::getMhtmlContent);
        return crawlerPageResultMapper.selectOne(queryWrapper);
    }

    private String buildDownloadFileName(CrawlerPageResultRecord pageResult) {
        long taskId = pageResult.getTaskId() == null ? 0L : pageResult.getTaskId();
        int pageIndex = pageResult.getPageIndex() == null ? 0 : pageResult.getPageIndex();
        return String.format("task-%d-page-%02d.mhtml", taskId, pageIndex);
    }

    public static class MhtmlDownloadData {
        private final String fileName;
        private final byte[] content;

        public MhtmlDownloadData(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }
    }

    private void updateTaskNodeIdIfMissing(Long taskId, String nodeId) {
        if (taskId == null || nodeId == null || nodeId.isBlank()) {
            return;
        }

        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Task::getTaskId, taskId)
                .and(wrapper -> wrapper.isNull(Task::getNodeId)
                        .or()
                        .eq(Task::getNodeId, ""))
                .set(Task::getNodeId, nodeId);
        taskMapper.update(null, updateWrapper);
    }
}
