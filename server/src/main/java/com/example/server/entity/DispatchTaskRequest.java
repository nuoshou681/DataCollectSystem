package com.example.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DispatchTaskRequest {
    private Long userId;
    private String keyword;
    private String url;
    private Integer maxLinksPerLevel;
    private String siteType;
    private Integer priority;
    private String source;
    private String batchId;
    private String idempotencyKey;
}
