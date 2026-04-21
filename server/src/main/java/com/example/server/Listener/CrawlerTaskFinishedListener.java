package com.example.server.Listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Message.CrawlerTaskFinished;
import com.example.server.entity.Task;
import com.example.server.mapper.TaskMapper;

@Component
public class CrawlerTaskFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskFinishedListener.class);

    private final TaskMapper taskMapper;

    public CrawlerTaskFinishedListener(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_FINISHED_QUEUE)
    public void handleTaskFinished(CrawlerTaskFinished finished) {
        log.info("任务完成: taskId={}, nodeId={}, success={}, totalPages={}",
                finished.getTaskId(),
                finished.getNodeId(),
                finished.isSuccess(),
                finished.getTotalPages());

        if (finished.getTaskId() == null) {
            return;
        }

        String nextStatus = finished.isSuccess() ? "FINISHED" : "FAILED";
        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Task::getTaskId, finished.getTaskId())
                .set(Task::getTaskStatus, nextStatus)
                .set(Task::getTaskProgress, 100)
                .set(Task::getTotalPages, finished.getTotalPages())
                .set(Task::getUpdatedAt, LocalDateTime.now());

        if (finished.getNodeId() != null && !finished.getNodeId().isBlank()) {
            updateWrapper.set(Task::getNodeId, finished.getNodeId());
        }

        taskMapper.update(null, updateWrapper);
    }
}
