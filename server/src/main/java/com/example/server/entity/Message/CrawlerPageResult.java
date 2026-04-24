package com.example.server.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerPageResult {
    private Long taskId;
    private String nodeId; // 爬虫节点

    private String pageUrl;
    private String pageTitle;
    private int pageIndex;
    private Integer totalPages;

    private boolean success;
    private String filePath;
    private String errorMessage; // 失败原因（成功时可为空）
}