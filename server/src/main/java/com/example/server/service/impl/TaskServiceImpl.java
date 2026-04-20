package com.example.server.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.DispatchTaskRequest;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskMapper;
import com.example.server.service.TaskService;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskMapper taskMapper;
    private final RabbitTemplate rabbitTemplate;

    public TaskServiceImpl(TaskMapper taskMapper, RabbitTemplate rabbitTemplate) {
        this.taskMapper = taskMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public List<Task> dispatchTasks(DispatchTaskRequest request) {
        List<Task> tasks = buildTasks(request);

        // 保存任务并派发到MQ
        for (Task task : tasks) {
            task.setKeyword(request.getKeyword());
            taskMapper.insert(task);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CRAWLER_EXCHANGE,
                    RabbitMQConfig.ROUTING_TASK,
                    task);
        }

        return tasks;
    }

    @Override
    public List<Task> buildTasks(DispatchTaskRequest request) {
        if (request == null || request.getUrl() == null) {
            throw new IllegalArgumentException("task 或 url 不能为空");
        }

        String[] urls = request.getUrl().split("[,;\\n\\r]+");
        List<Task> tasks = new ArrayList<>();
        for (String url : urls) {
            url = url.trim();
            if (url.isEmpty()) {
                continue;
            }
            Task task = new Task();
            task.setUrl(url);
            tasks.add(task);
        }
        return tasks;
    }

}
