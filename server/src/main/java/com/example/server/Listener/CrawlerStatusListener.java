package com.example.server.Listener;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Message.CrawlerStatus;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CrawlerStatusListener {

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_STATUS_QUEUE)
    public void handleStatus(CrawlerStatus status) {
        // 更新节点状态表 / 状态面板
        System.out.println("收到节点状态" + status);
    }
}
