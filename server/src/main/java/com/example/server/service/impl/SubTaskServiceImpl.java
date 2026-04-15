package com.example.server.service.impl;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.SubTask;
import com.example.server.service.SubTaskService;

@Service
public class SubTaskServiceImpl implements SubTaskService {
    private final RabbitTemplate rabbitTemplate;

    public SubTaskServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void dispatchSubTasks(List<SubTask> subTasks) {
        if (subTasks == null || subTasks.size() == 0) {
            return;
        }
        for (SubTask subTask : subTasks) {
            if (subTask == null) {
                continue;
            }
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CRAWLER_EXCHANGE,  // 交换机
                    RabbitMQConfig.ROUTING_TASK,      // 路由键，绑定到 crawler.task.queue
                    subTask                           // 消息体（子任务对象）
            );
        }
    }
}
