package com.example.server.controller;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.Task;
import com.example.server.service.impl.TaskServiceImpl;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/task/import")
public class TaskImportController {
    private final TaskServiceImpl taskService;

    public TaskImportController(TaskServiceImpl taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/csv")
    public ApiResponse<?> importCsv(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false, defaultValue = "10") Integer maxLinksPerLevel) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (file.isEmpty()) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        List<Task> tasks = new ArrayList<>();
        int total = 0;
        int imported = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirst = true;
            while ((line = reader.readLine()) != null) {
                total++;
                if (isFirst) { isFirst = false; continue; } // skip header
                String[] cols = line.split(",");
                if (cols.length < 2) continue;

                String keyword = cols[0].trim();
                String url = cols.length > 1 ? cols[1].trim() : "";
                String siteType = cols.length > 2 ? cols[2].trim() : null;

                DispatchTaskRequest req = new DispatchTaskRequest();
                req.setUserId(userId);
                req.setKeyword(keyword);
                req.setUrl(url);
                req.setSiteType(siteType);
                req.setMaxLinksPerLevel(maxLinksPerLevel);
                req.setSource("import");
                tasks.addAll(taskService.dispatchTasks(req));
                imported++;
            }
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.SERVER_ERROR, "解析文件失败: " + e.getMessage());
        }

        return ApiResponse.success(java.util.Map.of(
                "total", total - 1,
                "imported", imported,
                "tasksCreated", tasks.size()
        ));
    }
}
