package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("crawler_page_result")
public class CrawlerPageResultRecord {
    @TableId(value = "page_result_id", type = IdType.AUTO)
    private Long pageResultId;

    @TableField("task_id")
    private Long taskId;
    private String nodeId;

    private String pageUrl;
    private String pageTitle;
    private Integer pageIndex;

    private Boolean success;
    private String filePath;
    private String errorMessage;

    private Boolean mhtmlCached;
    private LocalDateTime mhtmlCachedAt;

    @JsonIgnore
    @TableField(select = false)
    private String mhtmlContent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
