package com.example.server.service;

import com.example.server.entity.SubTask;
import com.example.server.config.RabbitMQConfig;
// ...existing code...
import com.example.server.entity.Crawler;
import com.example.server.mapper.SubTaskMapper;
import com.example.server.mapper.ClientMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
        // 检测是否有可用的爬虫节点
        List<Crawler> clients = clientMapper.selectList(null);
        int clientCount = clients.size();
        if (clientCount == 0) {
            return false;
        }
        // 分发子任务到任务队列
        List<Long> insertedIds = new ArrayList<>();
        try {
            // 需要修改子任务表，在这里设置子任务的状态（正在执行，等待执行，执行完成）
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
