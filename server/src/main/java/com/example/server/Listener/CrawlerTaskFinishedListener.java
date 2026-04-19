package com.example.server.Listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.SubTask;
import com.example.server.entity.Task;
import com.example.server.entity.Message.CrawlerTaskFinished;
import com.example.server.mapper.SubTaskMapper;
import com.example.server.mapper.TaskMapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CrawlerTaskFinishedListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerTaskFinishedListener.class);

    private final TaskMapper taskMapper;
    private final SubTaskMapper subTaskMapper;

    private final ConcurrentHashMap<Long, Set<Long>> finishedSubtasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Long>> failedSubtasks = new ConcurrentHashMap<>();

    public CrawlerTaskFinishedListener(TaskMapper taskMapper, SubTaskMapper subTaskMapper) {
        this.taskMapper = taskMapper;
        this.subTaskMapper = subTaskMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_TASK_FINISHED_QUEUE)
    public void handleTaskFinished(CrawlerTaskFinished finished) {
        log.info("子任务完成: taskId={}, subTaskId={}, nodeId={}, success={}, totalPages={}",
                finished.getTaskId(),
                finished.getSubTaskId(),
                finished.getNodeId(),
                finished.isSuccess(),
                finished.getTotalPages());

        if (finished.getTaskId() == null || finished.getSubTaskId() == null) {
            return;
        }

        finishedSubtasks.computeIfAbsent(finished.getTaskId(), key -> ConcurrentHashMap.newKeySet())
                .add(finished.getSubTaskId());

        if (!finished.isSuccess()) {
            failedSubtasks.computeIfAbsent(finished.getTaskId(), key -> ConcurrentHashMap.newKeySet())
                    .add(finished.getSubTaskId());
        }

        LambdaQueryWrapper<SubTask> subTaskCountQuery = new LambdaQueryWrapper<>();
        subTaskCountQuery.eq(SubTask::getTaskId, finished.getTaskId());
        Long totalSubTasks = subTaskMapper.selectCount(subTaskCountQuery);

        int completed = finishedSubtasks.get(finished.getTaskId()).size();
        int failed = failedSubtasks.getOrDefault(finished.getTaskId(), Set.of()).size();

        int progress = 0;
        if (totalSubTasks != null && totalSubTasks > 0) {
            progress = Math.min(100, (int) ((completed * 100L) / totalSubTasks));
        }

        String status = "RUNNING";
        if (totalSubTasks != null && totalSubTasks > 0 && completed >= totalSubTasks) {
            if (failed == 0) {
                status = "FINISHED";
            } else if (failed >= completed) {
                status = "FAILED";
            } else {
                status = "PARTIAL_FAILED";
            }
        }

        LambdaUpdateWrapper<Task> updateTask = new LambdaUpdateWrapper<>();
        updateTask.eq(Task::getTaskId, finished.getTaskId())
                .set(Task::getProgress, progress)
                .set(Task::getStatus, status);
        taskMapper.update(null, updateTask);
    }
}
