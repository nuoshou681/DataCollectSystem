package com.example.server.entity;

import lombok.Data;
import com.example.server.common.enums.ClientStatusType;

@Data
public class CrawlerStatus {
    private String nodeId; // 节点唯一标识：主机名 + 端口
    private ClientStatusType status; // ONLINE / BUSY / OFFLINE
    private long timestamp; // 上报时间
}
