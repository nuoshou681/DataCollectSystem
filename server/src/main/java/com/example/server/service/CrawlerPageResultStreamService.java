package com.example.server.service;

import com.example.server.entity.CrawlerPageResultRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CrawlerPageResultStreamService {

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

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
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException e) {
                disconnected.add(emitter);
            }
        }

        if (!disconnected.isEmpty()) {
            emitters.removeAll(disconnected);
        }
    }
}
