package com.example.server.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerPageResult {
    private Long taskId;
    private Long subTaskId;
    private String nodeId;

    private String pageUrl;
    private String pageTitle;
    private int depth;

    private boolean success;
    private String content;
    private String snapshotPath;
    private String errorMessage;
}