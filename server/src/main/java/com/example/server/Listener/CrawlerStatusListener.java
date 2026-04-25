package com.example.server.Listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.Crawler;
import com.example.server.entity.Message.CrawlerStatus;
import com.example.server.mapper.ClientMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CrawlerStatusListener {

    private final ClientMapper clientMapper;

    public CrawlerStatusListener(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    @RabbitListener(queues = com.example.server.config.RabbitMQConfig.CRAWLER_STATUS_QUEUE)
    public void handleStatus(CrawlerStatus status) {
        if (status == null || status.getNodeId() == null || status.getNodeId().isBlank()) {
            return;
        }

        LocalDateTime heartbeat = LocalDateTime.ofInstant(Instant.ofEpochMilli(status.getTimestamp()), ZoneId.systemDefault());
        Crawler crawler = clientMapper.selectOne(new LambdaQueryWrapper<Crawler>()
                .eq(Crawler::getNodeId, status.getNodeId()));

        if (crawler == null) {
            crawler = new Crawler();
            crawler.setNodeId(status.getNodeId());
        }

        crawler.setNodeName(status.getNodeName());
        crawler.setVersion(status.getVersion());
        crawler.setCapabilitiesJson(writeJsonArray(status.getCapabilities()));
        crawler.setTagsJson(writeJsonArray(status.getTags()));
        crawler.setMaxConcurrency(status.getMaxConcurrency() == null ? 1 : status.getMaxConcurrency());
        crawler.setCurrentLoad(status.getCurrentLoad() == null ? 0 : status.getCurrentLoad());
        crawler.setHeartbeatTimeoutSec(status.getHeartbeatTimeoutSec() == null ? 15 : status.getHeartbeatTimeoutSec());
        crawler.setStatus(status.getStatus() == null ? "ONLINE" : status.getStatus().name());
        crawler.setLastHeartbeat(heartbeat);
        crawler.setLastOnlineAt(heartbeat);

        if (clientMapper.selectById(crawler.getNodeId()) == null) {
            clientMapper.insert(crawler);
            return;
        }
        clientMapper.updateById(crawler);
    }

    private String writeJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return "[" + values.stream()
                .map(value -> "\"" + escapeJson(value) + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "]";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
