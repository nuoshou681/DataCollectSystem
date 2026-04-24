package com.example.server.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerTaskMessage {
    private Long taskId;
    private String nodeId;
    private String url;
    private String keyword;
    private int maxLinksPerLevel = 10;
}
