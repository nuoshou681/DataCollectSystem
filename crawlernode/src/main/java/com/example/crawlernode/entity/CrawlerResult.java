package com.example.crawlernode.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerResult {
    private String nodeId;
    private boolean  success;
    // 爬取图片、文字、视频等数据 结果用枚举更好后续再继续完善
    private String data;
}
