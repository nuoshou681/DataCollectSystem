package com.example.server.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.example.server.entity.SubTask;
import com.example.server.config.RabbitMQConfig;

@Service
public class TaskDispatchService {

    private final RabbitTemplate rabbitTemplate;

    public TaskDispatchService(RabbitTemplate rabbitTemplate, RabbitMQConfig rabbitMQConfig) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void dispatchTask(SubTask subtask) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_TASK,
                subtask);
    }
}
