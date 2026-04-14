package com.example.crawlernode.crawler;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.CrawlerStatus;
import com.example.crawlernode.enums.ClientStatusType;

@Component
public class CrawlerStatusReport {
    private final RabbitTemplate rabbitTemplate;

    @Value("${node.id}")
    private String nodeId;
    
    public CrawlerStatusReport(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // 每 5 秒上报一次状态信息
    @Scheduled(fixedDelay = 5000)
    public void statusReport() {
        CrawlerStatus status = new CrawlerStatus();
        status.setNodeId(nodeId);
        status.setStatus(ClientStatusType.ONLINE);
        status.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_STATUS,
                status);
    }

}
