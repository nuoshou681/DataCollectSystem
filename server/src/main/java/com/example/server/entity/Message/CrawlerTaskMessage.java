package com.example.server.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerTaskMessage {
    private Long taskId;
    private Long userId;
    private String nodeId;
    private String batchId;
    private String url;
    private String keyword;
    private String siteType;
    private Integer maxLinksPerLevel;
    private Integer priority;
    private String source;
    private String idempotencyKey;
    private java.util.List<Long> tagIds;
}
