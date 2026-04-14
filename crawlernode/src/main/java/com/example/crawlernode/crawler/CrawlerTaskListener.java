package com.example.crawlernode.crawler;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.CrawlerResult;
import com.example.crawlernode.entity.SubTask;

@Component
public class CrawlerTaskListener {

    private final Crawler crawler;
    private final RabbitTemplate rabbitTemplate;

    public CrawlerTaskListener(Crawler crawler, RabbitTemplate rabbitTemplate) {
        this.crawler = crawler;
        this.rabbitTemplate = rabbitTemplate;
    }

    // 从任务队列中获取消费任务
    @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_QUEUE)
    public void handleTask(SubTask subTask) {
        // 启用爬虫
        CrawlerResult result = crawler.crawl(subTask);

        // 发送爬虫结果到结果队列
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_RESULT,
                result);
    }

}