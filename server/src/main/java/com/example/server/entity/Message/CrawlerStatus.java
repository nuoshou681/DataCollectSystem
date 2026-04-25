package com.example.server.entity.Message;

import com.example.server.common.enums.CrawlerStatusType;
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
    private CrawlerStatusType status;
    private long timestamp;
}
