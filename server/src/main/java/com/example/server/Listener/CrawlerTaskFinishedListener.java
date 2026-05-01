package com.example.server.Listener;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Message.CrawlerTaskFinished;
import com.example.server.service.NotificationService;
import com.example.server.service.TaskEventService;
import com.example.server.service.TaskRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CrawlerTaskFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskFinishedListener.class);

    private final TaskRuntimeService taskRuntimeService;
    private final TaskEventService taskEventService;
    private final NotificationService notificationService;

    public CrawlerTaskFinishedListener(TaskRuntimeService taskRuntimeService, TaskEventService taskEventService, NotificationService notificationService) {
        this.taskRuntimeService = taskRuntimeService;
        this.taskEventService = taskEventService;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_FINISHED_QUEUE)
    public void handleTaskFinished(CrawlerTaskFinished finished) {
        if (finished == null || finished.getTaskId() == null) {
            return;
        }

        log.info("任务完成: taskId={}, nodeId={}, success={}, totalPages={}",
                finished.getTaskId(),
                finished.getNodeId(),
                finished.isSuccess(),
                finished.getTotalPages());

        taskRuntimeService.markFinished(
                finished.getTaskId(),
                finished.getNodeId(),
                finished.isSuccess(),
                finished.getTotalPages(),
                finished.getSuccessPages(),
                finished.getFailedPages(),
                finished.getErrorCode(),
                finished.getMessage());
        taskEventService.recordEvent(
                finished.getTaskId(),
                finished.getNodeId(),
                "TASK_FINISHED",
                finished.isSuccess() ? "INFO" : "ERROR",
                finished.getMessage() == null || finished.getMessage().isBlank() ? "任务执行结束" : finished.getMessage(),
                "{\"successPages\":" + finished.getSuccessPages()
                        + ",\"failedPages\":" + finished.getFailedPages()
                        + ",\"errorCode\":\"" + (finished.getErrorCode() == null ? "" : finished.getErrorCode()) + "\"}");

        String notifTitle = finished.isSuccess()
                ? "任务执行完成"
                : "任务执行失败";
        String notifContent = finished.isSuccess()
                ? String.format("任务 #%d 已完成，成功 %d 页，失败 %d 页", finished.getTaskId(), finished.getSuccessPages(), finished.getFailedPages())
                : String.format("任务 #%d 执行失败：%s", finished.getTaskId(), finished.getMessage() != null ? finished.getMessage() : "未知错误");
        notificationService.createForTask(finished.getTaskId(), "TASK_FINISHED", notifTitle, notifContent);
    }
}
