package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("task_file")
public class TaskFile {
    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;

    @TableField("task_id")
    private Long taskId;

    @TableField("page_result_id")
    private Long pageResultId;

    @TableField("file_type")
    private String fileType;

    @TableField("storage_type")
    private String storageType;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_path")
    private String filePath;

    @TableField("content_sha256")
    private String contentSha256;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("db_content")
    private String dbContent;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
