package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("crawler_page_result")
public class CrawlerPageResultRecord {
    @TableId(value = "page_result_id", type = IdType.AUTO)
    private Long pageResultId;

    @TableField("task_id")
    private Long taskId;

    @TableField("node_id")
    private String nodeId;

    @TableField("site_type")
    private String siteType;

    @TableField("page_url")
    private String pageUrl;

    @TableField("page_title")
    private String pageTitle;

    @TableField("total_pages")
    private Integer totalPages;

    @TableField("page_index")
    private Integer pageIndex;

    private Boolean success;

    @TableField("file_path")
    private String filePath;

    @TableField("storage_type")
    private String storageType;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_size_bytes")
    private Long fileSizeBytes;

    @TableField("content_sha256")
    private String contentSha256;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("mhtml_cached")
    private Boolean mhtmlCached;

    @TableField("mhtml_cached_at")
    private LocalDateTime mhtmlCachedAt;

    @JsonIgnore
    @TableField(value = "mhtml_content", select = false)
    private String mhtmlContent;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
