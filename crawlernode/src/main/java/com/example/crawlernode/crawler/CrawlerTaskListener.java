package com.example.crawlernode.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.Task;

@Component
public class CrawlerTaskListener {

        private final Crawler crawler;

        @Value("${node.id}")
        private String nodeId;
        private static final Logger log = LoggerFactory.getLogger(CrawlerTaskListener.class);

        public CrawlerTaskListener(Crawler crawler) {
                this.crawler = crawler;
        }

        // 从任务队列中获取消费任务
        @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_QUEUE)
        public void handleTask(Task task) {
                // 1.收到任务
                log.info("【CrawlerNode】{} 收到任务: taskId={}, url={}, keyword={}",
                                nodeId, task.getTaskId(), task.getUrl(),
                                task.getKeyword());

                try {
                        crawler.crawl(task);
                        log.info("【CrawlerNode】{} 任务执行结束: taskId={}",
                                        nodeId, task.getTaskId());
                } catch (Exception e) {
                        log.error("【CrawlerNode】{} 任务执行失败: taskId={}, error={}",
                                        nodeId, task.getTaskId(), e.getMessage(), e);
                        crawler.reportTaskFinished(task, false, 0, e.getMessage());
                }
        }

}