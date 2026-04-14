package com.example.crawlernode.config;

// 保存队列配置的信息
public class RabbitMQConfig {
    public static final String CRAWLER_EXCHANGE = "crawler.exchange";
    public static final String CRAWLER_TASK_QUEUE = "crawler.task.queue";
    public static final String CRAWLER_RESULT_QUEUE = "crawler.result.queue";
    public static final String CRAWLER_STATUS_QUEUE = "crawler.status.queue";

    public static final String ROUTING_TASK = "crawler.task";
    public static final String ROUTING_RESULT = "crawler.result";
    public static final String ROUTING_STATUS = "crawler.status";
}
