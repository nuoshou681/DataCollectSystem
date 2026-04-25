package com.example.server.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskBatchDetailView {
    private TaskBatch batch;
    private List<Task> tasks;
}
