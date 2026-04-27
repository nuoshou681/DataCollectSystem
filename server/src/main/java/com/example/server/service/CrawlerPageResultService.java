package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Task;
import com.example.server.entity.Message.CrawlerPageResult;
import com.example.server.mapper.CrawlerPageResultMapper;
import com.example.server.mapper.TaskMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CrawlerPageResultService {

    private final CrawlerPageResultMapper crawlerPageResultMapper;
    private final TaskMapper taskMapper;
    private final TaskRuntimeService taskRuntimeService;
    private final TaskEventService taskEventService;
    private final TaskFileService taskFileService;
    private final TaskTagBindingService taskTagBindingService;

    public CrawlerPageResultService(
            CrawlerPageResultMapper crawlerPageResultMapper,
            TaskMapper taskMapper,
            TaskRuntimeService taskRuntimeService,
            TaskEventService taskEventService,
            TaskFileService taskFileService,
            TaskTagBindingService taskTagBindingService) {
        this.crawlerPageResultMapper = crawlerPageResultMapper;
        this.taskMapper = taskMapper;
        this.taskRuntimeService = taskRuntimeService;
        this.taskEventService = taskEventService;
        this.taskFileService = taskFileService;
        this.taskTagBindingService = taskTagBindingService;
    }

    public CrawlerPageResultRecord saveOrUpdateFromMessage(CrawlerPageResult message) {
        if (message == null || message.getTaskId() == null
                || message.getPageUrl() == null
                || message.getPageUrl().isBlank()) {
            return null;
        }

        if (!taskExists(message.getTaskId())) {
            return null;
        }

        LambdaQueryWrapper<CrawlerPageResultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CrawlerPageResultRecord::getTaskId, message.getTaskId())
                .eq(CrawlerPageResultRecord::getPageIndex, message.getPageIndex())
                .eq(CrawlerPageResultRecord::getPageUrl, message.getPageUrl());

        CrawlerPageResultRecord exists = crawlerPageResultMapper.selectOne(queryWrapper);
        if (exists == null) {
            CrawlerPageResultRecord insert = mapMessageToRecord(message, new CrawlerPageResultRecord());
            insert.setMhtmlCached(false);
            crawlerPageResultMapper.insert(insert);
            CrawlerPageResultRecord saved = crawlerPageResultMapper.selectById(insert.getPageResultId());
            afterPersist(saved, null);
            return saved;
        }

        Boolean previousSuccess = exists.getSuccess();
        CrawlerPageResultRecord update = mapMessageToRecord(message, exists);
        crawlerPageResultMapper.updateById(update);
        CrawlerPageResultRecord saved = crawlerPageResultMapper.selectById(update.getPageResultId());
        afterPersist(saved, previousSuccess);
        return saved;
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

    public List<CrawlerPageResultRecord> queryResults(Long taskId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return queryResults(taskId);
        }

        if (userId == null) {
            return List.of();
        }

        if (taskId != null) {
            Task task = taskMapper.selectById(taskId);
            if (task == null || !userId.equals(task.getUserId())) {
                return List.of();
            }
            return queryResults(taskId);
        }

        List<Long> taskIds = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .select(Task::getTaskId))
                .stream()
                .map(Task::getTaskId)
                .collect(Collectors.toList());

        if (taskIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<CrawlerPageResultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CrawlerPageResultRecord::getTaskId, taskIds)
                .orderByAsc(CrawlerPageResultRecord::getTaskId)
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

        CrawlerPageResultRecord saved = crawlerPageResultMapper.selectById(pageResultId);
        taskFileService.upsertFromPageResult(saved);
        taskEventService.recordEvent(saved.getTaskId(), saved.getNodeId(), "PAGE_MHTML_CACHED", "INFO", "页面 MHTML 已缓存到数据库", null);
        return saved;
    }

    public CrawlerPageResultRecord cacheMhtml(Long pageResultId, Long userId, boolean isAdmin) throws IOException {
        if (!canAccessPageResult(pageResultId, userId, isAdmin)) {
            return null;
        }
        return cacheMhtml(pageResultId);
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

    public List<MhtmlDownloadData> loadMhtmlFilesForExport(Long taskId, Long userId, boolean isAdmin) throws IOException {
        List<CrawlerPageResultRecord> records = queryResults(taskId, userId, isAdmin).stream()
                .filter(record -> Boolean.TRUE.equals(record.getSuccess()))
                .filter(record -> record.getPageResultId() != null)
                .toList();
        List<MhtmlDownloadData> result = new java.util.ArrayList<>();
        for (CrawlerPageResultRecord record : records) {
            MhtmlDownloadData item = loadMhtmlForDownload(record.getPageResultId(), userId, isAdmin);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    public MhtmlDownloadData loadMhtmlForDownload(Long pageResultId, Long userId, boolean isAdmin) throws IOException {
        if (!canAccessPageResult(pageResultId, userId, isAdmin)) {
            return null;
        }
        return loadMhtmlForDownload(pageResultId);
    }

    public boolean taskExists(Long taskId) {
        return taskId != null && taskMapper.selectById(taskId) != null;
    }

    public boolean canAccessTask(Long taskId, Long userId, boolean isAdmin) {
        if (taskId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return isAdmin || (userId != null && userId.equals(task.getUserId()));
    }

    public boolean canAccessPageResult(Long pageResultId, Long userId, boolean isAdmin) {
        if (pageResultId == null) {
            return false;
        }
        CrawlerPageResultRecord pageResult = crawlerPageResultMapper.selectById(pageResultId);
        if (pageResult == null) {
            return false;
        }
        return canAccessTask(pageResult.getTaskId(), userId, isAdmin);
    }

    private void afterPersist(CrawlerPageResultRecord record, Boolean previousSuccess) {
        taskRuntimeService.syncWithPageResults(
                record.getTaskId(),
                record.getNodeId(),
                record.getTotalPages(),
                Boolean.TRUE.equals(record.getSuccess()),
                record.getErrorCode(),
                record.getErrorMessage());
        taskFileService.upsertFromPageResult(record);
        Task task = taskMapper.selectById(record.getTaskId());
        if (task != null && task.getUserId() != null && record.getPageResultId() != null) {
            taskTagBindingService.inheritTaskTagsToPageResult(task.getTaskId(), record.getPageResultId(), task.getUserId());
        }
        if (previousSuccess == null || previousSuccess.booleanValue() != Boolean.TRUE.equals(record.getSuccess())) {
            taskEventService.recordEvent(
                    record.getTaskId(),
                    record.getNodeId(),
                    Boolean.TRUE.equals(record.getSuccess()) ? "PAGE_CAPTURED" : "PAGE_CAPTURE_FAILED",
                    Boolean.TRUE.equals(record.getSuccess()) ? "INFO" : "ERROR",
                    Boolean.TRUE.equals(record.getSuccess()) ? "页面采集成功" : "页面采集失败",
                    "{\"pageIndex\":" + record.getPageIndex() + ",\"pageUrl\":\"" + escapeJson(record.getPageUrl()) + "\"}");
        }
    }

    private CrawlerPageResultRecord mapMessageToRecord(CrawlerPageResult message, CrawlerPageResultRecord record) {
        record.setTaskId(message.getTaskId());
        record.setNodeId(message.getNodeId());
        record.setSiteType(message.getSiteType());
        record.setPageUrl(message.getPageUrl());
        record.setPageTitle(message.getPageTitle());
        record.setPageIndex(message.getPageIndex());
        record.setTotalPages(message.getTotalPages());
        record.setSuccess(message.isSuccess());
        record.setFilePath(message.getFilePath());
        record.setStorageType(message.getStorageType() == null ? "FILE" : message.getStorageType());
        record.setMimeType(message.getMimeType() == null ? "multipart/related" : message.getMimeType());
        record.setFileSizeBytes(message.getFileSizeBytes());
        record.setContentSha256(message.getContentSha256());
        record.setErrorCode(message.getErrorCode());
        record.setErrorMessage(message.getErrorMessage());
        return record;
    }

    private CrawlerPageResultRecord queryByIdForDownload(Long pageResultId) {
        LambdaQueryWrapper<CrawlerPageResultRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CrawlerPageResultRecord::getPageResultId, pageResultId)
                .select(
                        CrawlerPageResultRecord::getPageResultId,
                        CrawlerPageResultRecord::getTaskId,
                        CrawlerPageResultRecord::getPageIndex,
                        CrawlerPageResultRecord::getFilePath,
                        CrawlerPageResultRecord::getPageTitle,
                        CrawlerPageResultRecord::getMhtmlContent);
        return crawlerPageResultMapper.selectOne(queryWrapper);
    }

    private String buildDownloadFileName(CrawlerPageResultRecord pageResult) {
        long taskId = pageResult.getTaskId() == null ? 0L : pageResult.getTaskId();
        int pageIndex = pageResult.getPageIndex() == null ? 0 : pageResult.getPageIndex();
        return String.format("task-%d-page-%02d.mhtml", taskId, pageIndex);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
}
