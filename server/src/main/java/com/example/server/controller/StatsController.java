package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatsController {
    private final JdbcTemplate jdbcTemplate;

    public StatsController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();

        Map<String, Object> result = new LinkedHashMap<>();

        String taskWhere = isAdmin ? "" : " WHERE user_id = " + userId;
        result.put("totalTasks", q("SELECT COUNT(*) FROM task" + taskWhere));
        result.put("runningTasks", q("SELECT COUNT(*) FROM task" + (isAdmin ? "" : " WHERE user_id = " + userId) + (isAdmin ? " WHERE " : " AND ") + "task_status = 'RUNNING'"));
        result.put("finishedToday", q("SELECT COUNT(*) FROM task" + (isAdmin ? " WHERE " : " WHERE user_id = " + userId + " AND ") + "DATE(finished_at) = CURDATE()"));
        result.put("successRate", calcSuccessRate(userId, isAdmin));
        result.put("finishedTasks", q("SELECT COUNT(*) FROM task" + (isAdmin ? " WHERE " : " WHERE user_id = " + userId + " AND ") + "task_status = 'FINISHED'"));
        result.put("failedTasks", q("SELECT COUNT(*) FROM task" + (isAdmin ? " WHERE " : " WHERE user_id = " + userId + " AND ") + "task_status IN ('FAILED','PARTIAL_FAILED')"));
        result.put("pendingTasks", q("SELECT COUNT(*) FROM task" + (isAdmin ? " WHERE " : " WHERE user_id = " + userId + " AND ") + "task_status = 'PENDING'"));

        return ApiResponse.success(result);
    }

    @GetMapping("/trend")
    public ApiResponse<List<Map<String, Object>>> trend() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        String userFilter = isAdmin ? "" : " AND user_id = " + userId;

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day.toString());
            m.put("created", q("SELECT COUNT(*) FROM task WHERE DATE(created_at) = '" + day + "'" + userFilter));
            m.put("finished", q("SELECT COUNT(*) FROM task WHERE DATE(finished_at) = '" + day + "'" + userFilter));
            m.put("failed", q("SELECT COUNT(*) FROM task WHERE DATE(finished_at) = '" + day + "'" + userFilter + " AND task_status IN ('FAILED','PARTIAL_FAILED')"));
            list.add(m);
        }
        return ApiResponse.success(list);
    }

    @GetMapping("/site-distribution")
    public ApiResponse<List<Map<String, Object>>> siteDistribution() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        String where = isAdmin ? "" : " WHERE user_id = " + userId;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT COALESCE(site_type,'未知') as name, COUNT(*) as value FROM task" +
            (where.isEmpty() ? " GROUP BY site_type" : where + " GROUP BY site_type") +
            " ORDER BY value DESC"
        );
        return ApiResponse.success(rows);
    }

    @GetMapping("/node-load")
    public ApiResponse<List<Map<String, Object>>> nodeLoad() {
        if (!SecurityUtils.isAdmin()) {
            List<Map<String, Object>> empty = new ArrayList<>();
            return ApiResponse.success(empty);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT node_id as name, MAX(current_load) as value FROM crawler GROUP BY node_id"
        );
        return ApiResponse.success(rows);
    }

    @GetMapping("/hourly-activity")
    public ApiResponse<List<Map<String, Object>>> hourlyActivity() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        String userFilter = isAdmin ? "" : " AND user_id = " + userId;

        List<Map<String, Object>> list = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> m = new LinkedHashMap<>();
            String hour = String.format("%02d:00", h);
            m.put("hour", hour);
            m.put("count", q("SELECT COUNT(*) FROM task WHERE HOUR(created_at) = " + h + " AND DATE(created_at) = CURDATE()" + userFilter));
            list.add(m);
        }
        return ApiResponse.success(list);
    }

    private long q(String sql) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class);
        return v != null ? v : 0;
    }

    private double calcSuccessRate(Long userId, boolean isAdmin) {
        String userFilter = isAdmin ? "" : " AND user_id = " + userId;
        long total = q("SELECT COUNT(*) FROM task WHERE finished_at IS NOT NULL" + userFilter);
        if (total == 0) return 0;
        long success = q("SELECT COUNT(*) FROM task WHERE finished_at IS NOT NULL" + userFilter + " AND task_status = 'FINISHED'");
        return Math.round(success * 10000.0 / total) / 100.0;
    }
}
