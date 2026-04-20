package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerTaskFinished {
    private Long taskId;
    private String nodeId;

    private boolean success;
    private int totalPages;
    private String message;
}
