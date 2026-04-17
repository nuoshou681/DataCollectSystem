package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTask {
    private Long subtaskId;
    private Long taskId;
    // 分配爬虫节点id
    private String nodeId;
    private String url;
    private String keyword;
    // 每层最多爬多少个链接，防止爆炸
    private int maxLinksPerLevel = 10;
}
