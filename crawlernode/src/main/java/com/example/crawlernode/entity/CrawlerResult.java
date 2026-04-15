package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerResult {
    private Long taskId;
    private Long subTaskId;
    private String nodeId;       // 哪个爬虫节点处理的

    private boolean success;
    private String data;         // 结果内容（文本 / 保存路径 / JSON）
    private String errorMessage; // 失败原因（成功时可为空）
}
