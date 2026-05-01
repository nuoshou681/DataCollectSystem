package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/cleanup")
public class CleanupController {
    private final JdbcTemplate jdbcTemplate;

    public CleanupController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> cleanup(@RequestBody Map<String, Object> body) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        String scope = (String) body.getOrDefault("scope", "old_tasks");
        int days = body.containsKey("days") ? ((Number) body.get("days")).intValue() : 30;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        if ("old_tasks".equals(scope)) {
            // Clean old archived tasks
            String sql = "SELECT COUNT(*) FROM task WHERE archived = 1 AND updated_at < ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, cutoff);
            if (count != null && count > 0) {
                jdbcTemplate.update("DELETE FROM task WHERE archived = 1 AND updated_at < ?", cutoff);
                result.put("deletedArchivedTasks", count);
            } else {
                result.put("deletedArchivedTasks", 0);
            }

            // Clean old logs
            sql = "SELECT COUNT(*) FROM task_log WHERE created_at < ?";
            Integer logCount = jdbcTemplate.queryForObject(sql, Integer.class, cutoff);
            if (logCount != null && logCount > 0) {
                jdbcTemplate.update("DELETE FROM task_log WHERE created_at < ?", cutoff);
                result.put("deletedLogs", logCount);
            } else {
                result.put("deletedLogs", 0);
            }

            // Clean old events
            sql = "SELECT COUNT(*) FROM task_event WHERE created_at < ?";
            Integer eventCount = jdbcTemplate.queryForObject(sql, Integer.class, cutoff);
            if (eventCount != null && eventCount > 0) {
                jdbcTemplate.update("DELETE FROM task_event WHERE created_at < ?", cutoff);
                result.put("deletedEvents", eventCount);
            } else {
                result.put("deletedEvents", 0);
            }

            // Clean old notifications
            sql = "SELECT COUNT(*) FROM notification WHERE created_at < ?";
            Integer notifCount = jdbcTemplate.queryForObject(sql, Integer.class, cutoff);
            if (notifCount != null && notifCount > 0) {
                jdbcTemplate.update("DELETE FROM notification WHERE created_at < ?", cutoff);
                result.put("deletedNotifications", notifCount);
            } else {
                result.put("deletedNotifications", 0);
            }
        }

        result.put("scope", scope);
        result.put("cutoffBeforeDays", days);
        return ApiResponse.success(result);
    }

    @PostMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTasks", count("task"));
        result.put("totalPageResults", count("crawler_page_result"));
        result.put("totalLogs", count("task_log"));
        result.put("totalEvents", count("task_event"));
        result.put("totalNotifications", count("notification"));
        result.put("archivedTasks", countWhere("task", "archived = 1"));
        return ApiResponse.success(result);
    }

    private long count(String table) {
        Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return c != null ? c : 0;
    }

    private long countWhere(String table, String where) {
        Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
        return c != null ? c : 0;
    }
}
