package com.example.server.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Message.CrawlerTaskFinished;

@Component
public class CrawlerTaskFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskFinishedListener.class);

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_FINISHED_QUEUE)
    public void handleTaskFinished(CrawlerTaskFinished finished) {
        log.info("任务完成: taskId={}, nodeId={}, success={}, totalPages={}",
                finished.getTaskId(),
                finished.getNodeId(),
                finished.isSuccess(),
                finished.getTotalPages());
    }
}
