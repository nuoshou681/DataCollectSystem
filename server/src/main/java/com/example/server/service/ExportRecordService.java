package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.ExportRecord;
import com.example.server.mapper.ExportRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExportRecordService {
    private final ExportRecordMapper exportRecordMapper;

    public ExportRecordService(ExportRecordMapper exportRecordMapper) {
        this.exportRecordMapper = exportRecordMapper;
    }

    public ExportRecord create(Long userId, Long taskId, String exportType, String fileName, int recordCount) {
        ExportRecord record = new ExportRecord();
        record.setUserId(userId);
        record.setTaskId(taskId);
        record.setExportScope(taskId == null ? "RESULT_CENTER" : "TASK");
        record.setExportType(exportType);
        record.setFileName(fileName);
        record.setRecordCount(recordCount);
        record.setStatus("COMPLETED");
        record.setCreatedAt(LocalDateTime.now());
        exportRecordMapper.insert(record);
        return exportRecordMapper.selectById(record.getExportId());
    }

    public List<ExportRecord> list(Long userId, boolean isAdmin) {
        LambdaQueryWrapper<ExportRecord> query = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            query.eq(ExportRecord::getUserId, userId);
        }
        query.orderByDesc(ExportRecord::getCreatedAt);
        return exportRecordMapper.selectList(query);
    }
}
