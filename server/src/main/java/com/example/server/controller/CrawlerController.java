package com.example.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.Crawler;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.entity.Task;
import com.example.server.entity.TaskRuntime;
import com.example.server.mapper.ClientMapper;
import com.example.server.mapper.TaskMapper;
import com.example.server.mapper.TaskRuntimeMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrawlerController {

    @Autowired
    private ClientMapper clientmapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskRuntimeMapper taskRuntimeMapper;

    @GetMapping("/client")
    public ApiResponse<List<Crawler>> list() {
        LambdaQueryWrapper<Crawler> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Crawler::getLastHeartbeat);
        return ApiResponse.success(clientmapper.selectList(queryWrapper));
    }

    @GetMapping("/client/{nodeId}/tasks")
    public ApiResponse<Map<String, Object>> nodeTasks(@PathVariable String nodeId) {
        if (!SecurityUtils.isAdmin()) {
            return ApiResponse.error(ErrorCode.FORBIDDEN, "需要管理员权限");
        }

        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getNodeId, nodeId)
                        .orderByDesc(Task::getTaskId));

        for (Task task : tasks) {
            TaskRuntime runtime = taskRuntimeMapper.selectById(task.getTaskId());
            task.setRuntime(runtime);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nodeId", nodeId);
        data.put("tasks", tasks);
        data.put("total", tasks.size());
        return ApiResponse.success(data);
    }
}
