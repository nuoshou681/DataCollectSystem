package com.example.crawlernode.entity;

import com.example.crawlernode.enums.ClientStatusType;
import java.util.List;
import lombok.Data;

@Data
public class CrawlerStatus {
    private String nodeId;
    private String nodeName;
    private String version;
    private List<String> capabilities;
    private List<String> tags;
    private Integer maxConcurrency;
    private Integer currentLoad;
    private Integer heartbeatTimeoutSec;
    private ClientStatusType status;
    private long timestamp;
}
