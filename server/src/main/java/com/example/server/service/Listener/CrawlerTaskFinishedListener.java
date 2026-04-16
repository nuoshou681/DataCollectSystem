package com.example.server.service.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Message.CrawlerTaskFinished;

@Component
public class CrawlerTaskFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskFinishedListener.class);

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_RESULT_QUEUE)
    public void handleTaskFinished(CrawlerTaskFinished finished) {
        log.info("子任务完成: taskId={}, subTaskId={}, nodeId={}, success={}, totalPages={}",
                finished.getTaskId(),
                finished.getSubTaskId(),
                finished.getNodeId(),
                finished.isSuccess(),
                finished.getTotalPages());

        // 这里建议：
        // 1. 更新 subtask 状态为 FINISHED / FAILED
        // 2. 更新 user task 进度
        // 3. 如果 totalPages == 0，可以标记为空结果
    }
}
