package com.example.server.service;

import com.example.server.entity.SubTask;
import com.example.server.config.RabbitMQConfig;
// ...existing code...
import com.example.server.entity.Client;
import com.example.server.mapper.SubTaskMapper;
import com.example.server.mapper.ClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
// ...existing code...
import java.util.List;

@Service
public class SubTaskService {
    @Autowired
    private SubTaskMapper subTaskMapper;

    @Autowired
    private ClientMapper clientMapper;

    private final RabbitTemplate rabbitTemplate;

    public SubTaskService(RabbitTemplate rabbitTemplate)
    {
        this.rabbitTemplate = rabbitTemplate;
    }

    /* 分发子任务到可用客户端 */
    /**
     * 分发子任务到可用客户端，互斥访问，失败则回滚
     */
    public synchronized boolean dispatchSubTasks(List<SubTask> subTasks) {
        List<Client> clients = clientMapper.selectList(null);
        int clientCount = clients.size();
        if (clientCount == 0) {
            return false;
        }
        List<Long> insertedIds = new ArrayList<>();
        try {
            for (SubTask subTask : subTasks) {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.CRAWLER_EXCHANGE,
                        RabbitMQConfig.ROUTING_TASK,
                        subTask);
            }
            return true;
        } catch (Exception e) {
            // 回滚已插入的子任务
            for (Long id : insertedIds) {
                subTaskMapper.deleteById(id);
            }
            return false;
        }
    }
}
