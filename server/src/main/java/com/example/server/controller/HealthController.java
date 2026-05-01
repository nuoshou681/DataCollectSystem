package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/health")
public class HealthController {
    private final DataSource dataSource;
    private final RabbitAdmin rabbitAdmin;

    public HealthController(DataSource dataSource, RabbitAdmin rabbitAdmin) {
        this.dataSource = dataSource;
        this.rabbitAdmin = rabbitAdmin;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("mysql", checkDatabase());
        result.put("rabbitmq", checkRabbitMQ());
        result.put("jvm", getJvmInfo());
        result.put("disk", getDiskInfo());

        return ApiResponse.success(result);
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            JdbcTemplate jt = new JdbcTemplate(dataSource);
            jt.queryForObject("SELECT 1", Integer.class);
            m.put("status", "UP");
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> checkRabbitMQ() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            rabbitAdmin.getQueueProperties("crawler.task.queue");
            m.put("status", "UP");
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> getJvmInfo() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalMemoryMB", rt.totalMemory() / 1024 / 1024);
        m.put("freeMemoryMB", rt.freeMemory() / 1024 / 1024);
        m.put("usedMemoryMB", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        m.put("maxMemoryMB", rt.maxMemory() / 1024 / 1024);
        m.put("processors", rt.availableProcessors());
        return m;
    }

    private Map<String, Object> getDiskInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        java.io.File root = new java.io.File("/");
        m.put("totalGB", root.getTotalSpace() / 1024 / 1024 / 1024);
        m.put("freeGB", root.getFreeSpace() / 1024 / 1024 / 1024);
        m.put("usableGB", root.getUsableSpace() / 1024 / 1024 / 1024);
        return m;
    }
}
