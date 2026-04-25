package com.example.server.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDetailView {
    private Task task;
    private TaskRuntime runtime;
    private List<TaskEvent> events;
    private List<TaskFile> files;
    private List<CrawlerPageResultRecord> pageResults;
}
