package com.example.server.Listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Crawler;
import com.example.server.entity.Message.CrawlerStatus;
import com.example.server.mapper.ClientMapper;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class CrawlerStatusListener {

    private final ClientMapper clientMapper;

    public CrawlerStatusListener(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_STATUS_QUEUE)
    public void handleStatus(CrawlerStatus status) {
        if (status == null || status.getNodeId() == null || status.getNodeId().isBlank()) {
            return;
        }

        LocalDateTime heartbeat = LocalDateTime.ofInstant(Instant.ofEpochMilli(status.getTimestamp()),
                ZoneId.systemDefault());

        LambdaQueryWrapper<Crawler> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Crawler::getNodeId, status.getNodeId());
        Crawler exists = clientMapper.selectOne(queryWrapper);

        if (exists == null) {
            Crawler crawler = new Crawler();
            crawler.setNodeId(status.getNodeId());
            crawler.setStatus(String.valueOf(status.getStatus()));
            crawler.setLastHeartbeat(heartbeat);
            clientMapper.insert(crawler);
            return;
        }

        LambdaUpdateWrapper<Crawler> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Crawler::getNodeId, status.getNodeId())
                .set(Crawler::getStatus, String.valueOf(status.getStatus()))
                .set(Crawler::getLastHeartbeat, heartbeat);
        clientMapper.update(null, updateWrapper);
    }
}
