package com.example.crawlernode.crawler;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.CrawlerStatus;
import com.example.crawlernode.enums.ClientStatusType;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlerStatusReport {
    private final RabbitTemplate rabbitTemplate;

    @Value("${node.id}")
    private String nodeId;

    @Value("${node.name:${node.id}}")
    private String nodeName;

    @Value("${node.version:dev}")
    private String version;

    @Value("${node.max-concurrency:1}")
    private Integer maxConcurrency;

    @Value("${node.heartbeat-timeout-sec:15}")
    private Integer heartbeatTimeoutSec;

    public CrawlerStatusReport(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void statusReport() {
        CrawlerStatus status = new CrawlerStatus();
        status.setNodeId(nodeId);
        status.setNodeName(nodeName);
        status.setVersion(version);
        status.setCapabilities(List.of("MHTML_CAPTURE", "FILE_STORAGE", "PLAYWRIGHT", "FIRECRAWL_LINKS"));
        status.setTags(List.of("default", "playwright"));
        status.setMaxConcurrency(maxConcurrency);
        status.setCurrentLoad(CrawlerExecutionState.currentLoad());
        status.setHeartbeatTimeoutSec(heartbeatTimeoutSec);
        status.setStatus(CrawlerExecutionState.currentLoad() > 0 ? ClientStatusType.BUSY : ClientStatusType.ONLINE);
        status.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_STATUS,
                status);
    }
}
