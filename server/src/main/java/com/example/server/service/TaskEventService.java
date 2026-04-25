package com.example.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.server.entity.Task;
import com.example.server.entity.TaskEvent;
import com.example.server.entity.TaskLog;
import com.example.server.mapper.TaskEventMapper;
import com.example.server.mapper.TaskLogMapper;
import com.example.server.mapper.TaskMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskEventService {
    private final TaskEventMapper taskEventMapper;
    private final TaskLogMapper taskLogMapper;
    private final TaskMapper taskMapper;

    public TaskEventService(TaskEventMapper taskEventMapper, TaskLogMapper taskLogMapper, TaskMapper taskMapper) {
        this.taskEventMapper = taskEventMapper;
        this.taskLogMapper = taskLogMapper;
        this.taskMapper = taskMapper;
    }

    public void recordEvent(Long taskId, String nodeId, String eventType, String eventLevel, String message, String payloadJson) {
        if (taskId == null || eventType == null || eventType.isBlank() || message == null || message.isBlank()) {
            return;
        }

        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setNodeId(nodeId);
        event.setEventType(eventType);
        event.setEventLevel(eventLevel == null || eventLevel.isBlank() ? "INFO" : eventLevel.toUpperCase());
        event.setEventMessage(message);
        event.setPayloadJson(payloadJson);
        taskEventMapper.insert(event);

        TaskLog log = new TaskLog();
        log.setTaskId(taskId);
        log.setNodeKey(nodeId);
        log.setLogLevel(event.getEventLevel());
        log.setLogMessage("[" + eventType + "] " + message);
        taskLogMapper.insert(log);
    }

    public List<TaskEvent> queryByTaskId(Long taskId, Long userId, boolean isAdmin) {
        if (!canAccessTask(taskId, userId, isAdmin)) {
            return List.of();
        }

        return taskEventMapper.selectList(new LambdaQueryWrapper<TaskEvent>()
                .eq(TaskEvent::getTaskId, taskId)
                .orderByAsc(TaskEvent::getEventId));
    }

    private boolean canAccessTask(Long taskId, Long userId, boolean isAdmin) {
        if (taskId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return isAdmin || (userId != null && userId.equals(task.getUserId()));
    }
}
