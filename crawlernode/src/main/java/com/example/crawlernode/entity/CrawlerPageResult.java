package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerPageResult {
    private Long taskId;
    private Long subTaskId;
    private String nodeId;       // 爬虫节点

    private String pageUrl;
    private String pageTitle;
    private int pageIndex;
    
    private boolean success;
    private String filePath;     
    private String errorMessage; // 失败原因（成功时可为空）
}
