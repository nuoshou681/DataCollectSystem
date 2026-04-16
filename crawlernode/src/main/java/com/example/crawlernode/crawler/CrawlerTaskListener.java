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
                        log.info("【CrawlerNode】{} 子任务递归执行已触发完成: subTaskId={}",
                                        nodeId, subTask.getSubtaskId());
                } catch (Exception e) {
                        log.error("【CrawlerNode】{} 子任务执行失败: subTaskId={}, error={}",
                                        nodeId, subTask.getSubtaskId(), e.getMessage(), e);
                        crawler.reportTaskFinished(subTask, false, 0, e.getMessage());
                }
                // // 2.执行爬虫
                // log.info("【CrawlerNode】{} 开始执行爬虫: subTaskId={}, 目标URL={}", nodeId,
                // subTask.getSubtaskId(),
                // subTask.getUrl());
                // CrawlerPageResult result = crawler.crawl(subTask);

                // // 3.爬虫执行完成（发送前打印信息）
                // log.info("【CrawlerNode】{} 爬虫执行完成: subTaskId={}, success={}, dataLength={},
                // error={}",
                // nodeId,
                // result.getSubTaskId(),
                // result.isSuccess(),
                // result.getData() != null ? result.getData().length() : 0,
                // result.getErrorMessage());
                // // 4.爬虫结果上传结果队列
                // rabbitTemplate.convertAndSend(
                // RabbitMQConfig.CRAWLER_EXCHANGE,
                // RabbitMQConfig.ROUTING_RESULT,
                // result);
                // // 5.打印
                // log.info("【CrawlerNode】{} 结果已发送到队列: exchange={}, routingKey={},
                // subTaskId={}", nodeId,
                // RabbitMQConfig.CRAWLER_EXCHANGE,
                // RabbitMQConfig.ROUTING_RESULT,
                // result.getSubTaskId());
        }

}