package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTask {
    private String url;
    private String keyword;

    // 最大递归深度
    private int maxDepth = 3;
    // 每层最多爬多少个链接，防止爆炸
    private int maxLinksPerLevel = 20;
}
