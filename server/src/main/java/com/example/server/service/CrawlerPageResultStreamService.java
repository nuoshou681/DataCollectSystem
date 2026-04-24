package com.example.server.service;

import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CrawlerPageResultStreamService {

    private final Map<SseEmitter, StreamSubscriber> emitters = new ConcurrentHashMap<>();

    private final TaskMapper taskMapper;

    public CrawlerPageResultStreamService(TaskMapper taskMapper) {
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

    public void publish(CrawlerPageResultRecord pageResult) {
        if (pageResult == null || emitters.isEmpty()) {
            return;
        }

        publishEvent("crawler-page-result", pageResult);
    }

    private void publishEvent(String eventName, Object payload) {
        if (eventName == null || eventName.isBlank() || payload == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> disconnected = new ArrayList<>();
        for (Map.Entry<SseEmitter, StreamSubscriber> entry : emitters.entrySet()) {
            SseEmitter emitter = entry.getKey();
            StreamSubscriber subscriber = entry.getValue();
            if (!canReceive(subscriber, payload)) {
                continue;
            }
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException e) {
                disconnected.add(emitter);
            }
        }

        if (!disconnected.isEmpty()) {
            disconnected.forEach(emitters::remove);
        }
    }

    private boolean canReceive(StreamSubscriber subscriber, Object payload) {
        if (!(payload instanceof CrawlerPageResultRecord pageResult)) {
            return false;
        }
        if (subscriber == null) {
            return false;
        }
        if (subscriber.isAdmin()) {
            return true;
        }
        if (subscriber.userId() == null || pageResult.getTaskId() == null) {
            return false;
        }

        com.example.server.entity.Task task = taskMapper.selectById(pageResult.getTaskId());
        return task != null && subscriber.userId().equals(task.getUserId());
    }

    private record StreamSubscriber(Long userId, boolean isAdmin) {
    }
}
