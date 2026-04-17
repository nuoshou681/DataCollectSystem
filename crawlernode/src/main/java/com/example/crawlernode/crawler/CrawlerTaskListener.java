package com.example.crawlernode.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.SubTask;

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
        public void handleTask(SubTask subTask) {
                // 1.收到任务
                log.info("【CrawlerNode】{} 收到子任务: taskId={}, subTaskId={}, url={}, keyword={}",
                                nodeId, subTask.getTaskId(), subTask.getSubtaskId(), subTask.getUrl(),
                                subTask.getKeyword());

                try {
                        crawler.crawl(subTask);
                        log.info("【CrawlerNode】{} 子任务执行结束: subTaskId={}",
                                        nodeId, subTask.getSubtaskId());
                } catch (Exception e) {
                        log.error("【CrawlerNode】{} 子任务执行失败: subTaskId={}, error={}",
                                        nodeId, subTask.getSubtaskId(), e.getMessage(), e);
                        crawler.reportTaskFinished(subTask, false, 0, e.getMessage());
                }
        }

}