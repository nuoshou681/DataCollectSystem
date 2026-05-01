package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.TaskSchedule;
import com.example.server.mapper.TaskScheduleMapper;
import com.example.server.service.impl.TaskServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskScheduleService {
    private final TaskScheduleMapper scheduleMapper;
    private final TaskServiceImpl taskService;

    public TaskScheduleService(TaskScheduleMapper scheduleMapper, TaskServiceImpl taskService) {
        this.scheduleMapper = scheduleMapper;
        this.taskService = taskService;
    }

    public List<TaskSchedule> listByUser(Long userId) {
        QueryWrapper<TaskSchedule> w = new QueryWrapper<>();
        w.eq("user_id", userId).orderByDesc("created_at");
        return scheduleMapper.selectList(w);
    }

    public TaskSchedule create(TaskSchedule s) {
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        if (s.getEnabled() == null) s.setEnabled(true);
        scheduleMapper.insert(s);
        return s;
    }

    public TaskSchedule update(TaskSchedule s, Long userId) {
        TaskSchedule existing = scheduleMapper.selectById(s.getScheduleId());
        if (existing == null || !existing.getUserId().equals(userId)) return null;
        existing.setScheduleName(s.getScheduleName());
        existing.setKeyword(s.getKeyword());
        existing.setUrl(s.getUrl());
        existing.setSiteType(s.getSiteType());
        existing.setMaxLinksPerLevel(s.getMaxLinksPerLevel());
        existing.setCronExpression(s.getCronExpression());
        existing.setEnabled(s.getEnabled());
        existing.setUpdatedAt(LocalDateTime.now());
        scheduleMapper.updateById(existing);
        return existing;
    }

    public boolean delete(Long scheduleId, Long userId) {
        QueryWrapper<TaskSchedule> w = new QueryWrapper<>();
        w.eq("schedule_id", scheduleId).eq("user_id", userId);
        return scheduleMapper.delete(w) > 0;
    }

    public boolean toggle(Long scheduleId, Long userId) {
        TaskSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null || !s.getUserId().equals(userId)) return false;
        s.setEnabled(!Boolean.TRUE.equals(s.getEnabled()));
        s.setUpdatedAt(LocalDateTime.now());
        scheduleMapper.updateById(s);
        return true;
    }

    @Scheduled(fixedDelay = 60000)
    public void checkAndDispatchScheduledTasks() {
        QueryWrapper<TaskSchedule> w = new QueryWrapper<>();
        w.eq("enabled", true);
        List<TaskSchedule> schedules = scheduleMapper.selectList(w);
        LocalDateTime now = LocalDateTime.now();

        for (TaskSchedule s : schedules) {
            try {
                boolean shouldRun = false;
                if (s.getLastRunAt() == null) {
                    shouldRun = true;
                } else {
                    String cron = s.getCronExpression();
                    if (cron == null || cron.isBlank()) continue;
                    long intervalMs = parseSimpleInterval(cron);
                    if (intervalMs > 0) {
                        shouldRun = java.time.Duration.between(s.getLastRunAt(), now).toMillis() >= intervalMs;
                    }
                }
                if (shouldRun) {
                    DispatchTaskRequest req = new DispatchTaskRequest();
                    req.setUserId(s.getUserId());
                    req.setKeyword(s.getKeyword());
                    req.setUrl(s.getUrl());
                    req.setSiteType(s.getSiteType());
                    req.setMaxLinksPerLevel(s.getMaxLinksPerLevel() != null ? s.getMaxLinksPerLevel() : 10);
                    req.setSource("scheduled");
                    taskService.dispatchTasks(req);
                    s.setLastRunAt(now);
                    s.setUpdatedAt(now);
                    scheduleMapper.updateById(s);
                }
            } catch (Exception e) {
                // log and continue
            }
        }
    }

    private long parseSimpleInterval(String cron) {
        cron = cron.trim();
        if (cron.startsWith("every_")) {
            try {
                long minutes = Long.parseLong(cron.substring(6));
                return minutes * 60 * 1000;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        try {
            return Long.parseLong(cron) * 60 * 1000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
