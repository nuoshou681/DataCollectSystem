package com.example.server.service;

import com.example.server.entity.Task;
import com.example.server.entity.TaskRuntime;
import com.example.server.mapper.TaskMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TaskRuntimeStreamService {

    private final Map<SseEmitter, StreamSubscriber> emitters = new ConcurrentHashMap<>();
    private final TaskMapper taskMapper;

    public TaskRuntimeStreamService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public SseEmitter subscribe(Long userId, boolean isAdmin) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(emitter, new StreamSubscriber(userId, isAdmin));

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void publish(TaskRuntime runtime) {
        if (runtime == null || runtime.getTaskId() == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> disconnected = new ArrayList<>();
        for (Map.Entry<SseEmitter, StreamSubscriber> entry : emitters.entrySet()) {
            if (!canReceive(entry.getValue(), runtime)) {
                continue;
            }
            try {
                entry.getKey().send(SseEmitter.event().name("task-runtime").data(runtime));
            } catch (IOException | IllegalStateException e) {
                disconnected.add(entry.getKey());
            }
        }

        disconnected.forEach(emitters::remove);
    }

    private boolean canReceive(StreamSubscriber subscriber, TaskRuntime runtime) {
        if (subscriber == null || runtime == null || runtime.getTaskId() == null) {
            return false;
        }
        if (subscriber.isAdmin()) {
            return true;
        }
        Task task = taskMapper.selectById(runtime.getTaskId());
        return task != null && subscriber.userId() != null && subscriber.userId().equals(task.getUserId());
    }

    private record StreamSubscriber(Long userId, boolean isAdmin) {
    }
}
