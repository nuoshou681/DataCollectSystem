package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.TaskNote;
import com.example.server.mapper.TaskNoteMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskNoteService {
    private final TaskNoteMapper taskNoteMapper;
    private final CrawlerPageResultService crawlerPageResultService;

    public TaskNoteService(TaskNoteMapper taskNoteMapper, CrawlerPageResultService crawlerPageResultService) {
        this.taskNoteMapper = taskNoteMapper;
        this.crawlerPageResultService = crawlerPageResultService;
    }

    public List<TaskNote> list(Long taskId, Long userId, boolean isAdmin) {
        if (!crawlerPageResultService.canAccessTask(taskId, userId, isAdmin)) {
            return List.of();
        }
        return taskNoteMapper.selectList(new LambdaQueryWrapper<TaskNote>()
                .eq(TaskNote::getTaskId, taskId)
                .orderByDesc(TaskNote::getUpdatedAt));
    }

    public TaskNote create(Long taskId, String noteContent, Long userId, boolean isAdmin) {
        if (!crawlerPageResultService.canAccessTask(taskId, userId, isAdmin) || noteContent == null || noteContent.isBlank()) {
            return null;
        }
        TaskNote note = new TaskNote();
        note.setTaskId(taskId);
        note.setUserId(userId);
        note.setNoteContent(noteContent.trim());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        taskNoteMapper.insert(note);
        return taskNoteMapper.selectById(note.getNoteId());
    }

    public TaskNote update(Long noteId, String noteContent, Long userId, boolean isAdmin) {
        if (noteId == null || noteContent == null || noteContent.isBlank()) {
            return null;
        }
        TaskNote existing = taskNoteMapper.selectById(noteId);
        if (existing == null || !crawlerPageResultService.canAccessTask(existing.getTaskId(), userId, isAdmin)) {
            return null;
        }
        if (!isAdmin && (userId == null || !userId.equals(existing.getUserId()))) {
            return null;
        }
        existing.setNoteContent(noteContent.trim());
        existing.setUpdatedAt(LocalDateTime.now());
        taskNoteMapper.updateById(existing);
        return taskNoteMapper.selectById(noteId);
    }

    public boolean delete(Long noteId, Long userId, boolean isAdmin) {
        if (noteId == null) {
            return false;
        }
        TaskNote existing = taskNoteMapper.selectById(noteId);
        if (existing == null || !crawlerPageResultService.canAccessTask(existing.getTaskId(), userId, isAdmin)) {
            return false;
        }
        if (!isAdmin && (userId == null || !userId.equals(existing.getUserId()))) {
            return false;
        }
        return taskNoteMapper.deleteById(noteId) > 0;
    }
}
