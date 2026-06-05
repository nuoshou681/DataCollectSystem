package com.example.server.Listener;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Message.CrawlerPageResult;
import com.example.server.service.CrawlerPageResultService;
import com.example.server.service.CrawlerPageResultStreamService;
import com.example.server.service.TaskRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CrawlerPageResultListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerPageResultListener.class);

    private final CrawlerPageResultService crawlerPageResultService;
    private final CrawlerPageResultStreamService crawlerPageResultStreamService;
    private final TaskRuntimeService taskRuntimeService;

    public CrawlerPageResultListener(
            CrawlerPageResultService crawlerPageResultService,
            CrawlerPageResultStreamService crawlerPageResultStreamService,
            TaskRuntimeService taskRuntimeService) {
        this.crawlerPageResultService = crawlerPageResultService;
        this.crawlerPageResultStreamService = crawlerPageResultStreamService;
        this.taskRuntimeService = taskRuntimeService;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_RESULT_QUEUE)
    public void handlePageResult(CrawlerPageResult result) {
        if (result == null || result.getTaskId() == null) {
            log.warn("丢弃无效页面结果消息: taskId 为空");
            return;
        }

        taskRuntimeService.markStarted(result.getTaskId(), result.getNodeId());

        if (!crawlerPageResultService.taskExists(result.getTaskId())) {
            log.warn("丢弃孤儿页面结果消息: taskId={} 不存在, url={}", result.getTaskId(), result.getPageUrl());
            return;
        }

        CrawlerPageResultRecord saved = crawlerPageResultService.saveOrUpdateFromMessage(result);
        taskRuntimeService.syncWithPageResults(
                result.getTaskId(),
                result.getNodeId(),
                result.getTotalPages(),
                result.isSuccess(),
                result.getErrorCode(),
                result.getErrorMessage());
        crawlerPageResultStreamService.publish(saved);
    }
}
