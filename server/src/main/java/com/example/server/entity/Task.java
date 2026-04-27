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
@TableName("task")
public class Task {
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    @TableField("user_id")
    private Long userId;

    @TableField("node_id")
    private String nodeId;

    @TableField("batch_id")
    private String batchId;

    private String url;
    private String keyword;

    @TableField("site_type")
    private String siteType;

    @TableField("task_status")
    private String taskStatus;

    @TableField("task_progress")
    private Integer taskProgress;

    @TableField("total_pages")
    private Integer totalPages;

    @TableField("max_links_per_level")
    private Integer maxLinksPerLevel;

    private Integer priority;
    private String source;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("cancel_requested")
    private Boolean cancelRequested;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private TaskRuntime runtime;

    @TableField(exist = false)
    private java.util.List<Long> tagIds;
}
