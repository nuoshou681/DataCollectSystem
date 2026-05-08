package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.TaskSchedule;
import com.example.server.mapper.TaskScheduleMapper;
import com.example.server.service.impl.TaskServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskScheduleService {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduleService.class);

    private static final Map<String, String> SITE_URL_MAP = Map.ofEntries(
        Map.entry("sohu", "https://search.sohu.com/?keyword="),
        Map.entry("SOHU", "https://search.sohu.com/?keyword="),
        Map.entry("bing", "https://www.bing.com/search?q="),
        Map.entry("BING", "https://www.bing.com/search?q="),
        Map.entry("baidu_baike", "https://baike.baidu.com/item/"),
        Map.entry("BAIDU_BAIKE", "https://baike.baidu.com/item/"),
        Map.entry("baike", "https://baike.baidu.com/item/"),
        Map.entry("wikipedia", "https://en.wikipedia.org/w/index.php?search="),
        Map.entry("WIKIPEDIA", "https://en.wikipedia.org/w/index.php?search="),
        Map.entry("tencent_news", "https://news.qq.com"),
        Map.entry("TENCENT_NEWS", "https://news.qq.com"),
        Map.entry("sina_news", "https://news.sina.com.cn"),
        Map.entry("SINA_NEWS", "https://news.sina.com.cn"),
        Map.entry("thepaper", "https://www.thepaper.cn"),
        Map.entry("THEPAPER", "https://www.thepaper.cn"),
        Map.entry("huanqiu", "https://www.huanqiu.com"),
        Map.entry("HUANQIU", "https://www.huanqiu.com"),
        Map.entry("chinanews", "https://www.chinanews.com.cn"),
        Map.entry("CHINANEWS", "https://www.chinanews.com.cn"),
        Map.entry("cctv_news", "https://news.cctv.com"),
        Map.entry("CCTV_NEWS", "https://news.cctv.com"),
        Map.entry("guancha", "https://www.guancha.cn"),
        Map.entry("GUANCHA", "https://www.guancha.cn")
    );

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
                    String url = s.getUrl();
                    if (url == null || url.isBlank()) {
                        url = SITE_URL_MAP.getOrDefault(s.getSiteType(), null);
                    }
                    if (url == null || url.isBlank()) {
                        log.warn("定时任务 #{} url 为空且无法根据 siteType={} 推导，跳过", s.getScheduleId(), s.getSiteType());
                        continue;
                    }
                    DispatchTaskRequest req = new DispatchTaskRequest();
                    req.setUserId(s.getUserId());
                    req.setKeyword(s.getKeyword());
                    req.setUrl(url);
                    req.setSiteType(s.getSiteType());
                    req.setMaxLinksPerLevel(s.getMaxLinksPerLevel() != null ? s.getMaxLinksPerLevel() : 10);
                    req.setSource("scheduled");
                    log.info("定时调度触发: scheduleId={}, keyword={}, url={}", s.getScheduleId(), s.getKeyword(), url);
                    taskService.dispatchTasks(req);
                    s.setLastRunAt(now);
                    s.setNextRunAt(now.plusMinutes(parseSimpleInterval(s.getCronExpression()) / 60000));
                    s.setUpdatedAt(now);
                    scheduleMapper.updateById(s);
                    log.info("定时调度完成: scheduleId={}", s.getScheduleId());
                }
            } catch (Exception e) {
                log.error("定时调度执行失败: scheduleId={}, keyword={}, error={}", s.getScheduleId(), s.getKeyword(), e.getMessage());
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
