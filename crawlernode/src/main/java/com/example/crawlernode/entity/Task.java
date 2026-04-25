package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    private Long taskId;
    private Long userId;
    private String nodeId;
    private String batchId;
    private String url;
    private String keyword;
    private String siteType;
    private Integer maxLinksPerLevel = 10;
    private Integer priority = 0;
    private String source;
    private String idempotencyKey;
}
