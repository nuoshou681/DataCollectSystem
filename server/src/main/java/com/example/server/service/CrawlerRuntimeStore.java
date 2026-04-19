package com.example.server.service;

import com.example.server.entity.Message.CrawlerPageResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CrawlerRuntimeStore {

    private final Map<Long, List<CrawlerPageResult>> byTaskId = new ConcurrentHashMap<>();
    private final Map<Long, List<CrawlerPageResult>> bySubTaskId = new ConcurrentHashMap<>();

    public void addPageResult(CrawlerPageResult result) {
        if (result == null || result.getTaskId() == null || result.getSubTaskId() == null) {
            return;
        }

        byTaskId.computeIfAbsent(result.getTaskId(), k -> Collections.synchronizedList(new ArrayList<>())).add(result);
        bySubTaskId.computeIfAbsent(result.getSubTaskId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(result);
    }

    public List<CrawlerPageResult> getByTaskId(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        return sortCopy(byTaskId.getOrDefault(taskId, List.of()));
    }

    public List<CrawlerPageResult> getBySubTaskId(Long subTaskId) {
        if (subTaskId == null) {
            return List.of();
        }
        return sortCopy(bySubTaskId.getOrDefault(subTaskId, List.of()));
    }

    public List<CrawlerPageResult> getAll() {
        List<CrawlerPageResult> all = new ArrayList<>();
        for (List<CrawlerPageResult> results : byTaskId.values()) {
            all.addAll(results);
        }
        all.sort(Comparator.comparing(CrawlerPageResult::getTaskId)
                .thenComparing(CrawlerPageResult::getSubTaskId)
                .thenComparingInt(CrawlerPageResult::getPageIndex));
        return all;
    }

    private List<CrawlerPageResult> sortCopy(List<CrawlerPageResult> source) {
        List<CrawlerPageResult> copy = new ArrayList<>(source);
        copy.sort(Comparator.comparing(CrawlerPageResult::getTaskId)
                .thenComparing(CrawlerPageResult::getSubTaskId)
                .thenComparingInt(CrawlerPageResult::getPageIndex));
        return copy;
    }
}
