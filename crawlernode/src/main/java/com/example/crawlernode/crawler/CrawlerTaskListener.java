package com.example.crawlernode.crawler;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CrawlerTaskListener {

    private final Crawler crawler;

    @Value("${node.id}")
    private String nodeId;

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskListener.class);

    public CrawlerTaskListener(Crawler crawler) {
        this.crawler = crawler;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_QUEUE)
    public void handleTask(Task task) {
        log.info("【CrawlerNode】{} 收到任务: taskId={}, url={}, keyword={}",
                nodeId, task.getTaskId(), task.getUrl(), task.getKeyword());

        CrawlerExecutionState.incrementLoad();
        try {
            crawler.crawl(task);
            log.info("【CrawlerNode】{} 任务执行结束: taskId={}", nodeId, task.getTaskId());
        } catch (Exception e) {
            log.error("【CrawlerNode】{} 任务执行失败: taskId={}, error={}",
                    nodeId, task.getTaskId(), e.getMessage(), e);
            crawler.reportTaskFinished(task, false, 0, 0, 0, "TASK_EXECUTION_ERROR", e.getMessage());
        } finally {
            CrawlerExecutionState.decrementLoad();
        }
    }
}
