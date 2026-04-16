package com.example.server.service.Listener;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.CrawlResult;
import com.example.server.entity.CrawlerStatus;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ResultAndStatusListener {

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_RESULT_QUEUE)
    public void handleResult(CrawlResult result) {
        // 保存到数据库/日志/前端
        System.out.println("收到爬虫结果: " + result);
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_STATUS_QUEUE)
    public void handleStatus(CrawlerStatus status) {
        // 更新节点状态表 / 状态面板
        System.out.println("收到节点状态" + status);
    }
}
