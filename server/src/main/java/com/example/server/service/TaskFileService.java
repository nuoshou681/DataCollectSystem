package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Task;
import com.example.server.entity.TaskFile;
import com.example.server.mapper.TaskFileMapper;
import com.example.server.mapper.TaskMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskFileService {
    private final TaskFileMapper taskFileMapper;
    private final TaskMapper taskMapper;

    public TaskFileService(TaskFileMapper taskFileMapper, TaskMapper taskMapper) {
        this.taskFileMapper = taskFileMapper;
        this.taskMapper = taskMapper;
    }

    public void upsertFromPageResult(CrawlerPageResultRecord record) {
        if (record == null || record.getTaskId() == null || record.getPageResultId() == null) {
            return;
        }
        LambdaQueryWrapper<TaskFile> query = new LambdaQueryWrapper<TaskFile>()
                .eq(TaskFile::getTaskId, record.getTaskId())
                .eq(TaskFile::getPageResultId, record.getPageResultId());
        TaskFile existing = taskFileMapper.selectOne(query);

        TaskFile file = existing == null ? new TaskFile() : existing;
        file.setTaskId(record.getTaskId());
        file.setPageResultId(record.getPageResultId());
        file.setFileType("MHTML");
        file.setStorageType(record.getStorageType());
        file.setMimeType(record.getMimeType());
        file.setFilePath(record.getFilePath());
        file.setContentSha256(record.getContentSha256());
        file.setSizeBytes(record.getFileSizeBytes());
        if (existing == null) {
            taskFileMapper.insert(file);
            return;
        }
        taskFileMapper.updateById(file);
    }

    public List<TaskFile> queryByTaskId(Long taskId, Long userId, boolean isAdmin) {
        if (!canAccessTask(taskId, userId, isAdmin)) {
            return List.of();
        }
        return taskFileMapper.selectList(new LambdaQueryWrapper<TaskFile>()
                .eq(TaskFile::getTaskId, taskId)
                .orderByAsc(TaskFile::getFileId));
    }

    private boolean canAccessTask(Long taskId, Long userId, boolean isAdmin) {
        if (taskId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        return task != null && (isAdmin || (userId != null && userId.equals(task.getUserId())));
    }
}
