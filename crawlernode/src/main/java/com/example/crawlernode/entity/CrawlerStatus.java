package com.example.crawlernode.entity;

import com.example.crawlernode.enums.ClientStatusType;

import lombok.Data;

@Data
public class CrawlerStatus {
    private String nodeId; // 节点唯一标识：主机名 + 端口
    private ClientStatusType status; // ONLINE / BUSY / OFFLINE
    private long timestamp; // 上报时间
}