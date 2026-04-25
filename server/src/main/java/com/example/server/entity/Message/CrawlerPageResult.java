package com.example.server.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawlerPageResult {
    private Long taskId;
    private String nodeId;
    private String siteType;
    private String pageUrl;
    private String pageTitle;
    private int pageIndex;
    private Integer totalPages;
    private boolean success;
    private String filePath;
    private String storageType;
    private String mimeType;
    private Long fileSizeBytes;
    private String contentSha256;
    private String errorCode;
    private String errorMessage;
}
