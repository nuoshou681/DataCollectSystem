package com.example.server.entity;

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
@TableName("task_runtime")
public class TaskRuntime {
    @TableId("task_id")
    private Long taskId;

    private String status;

    @TableField("assigned_node_id")
    private String assignedNodeId;

    @TableField("progress_percent")
    private Integer progressPercent;

    @TableField("expected_pages")
    private Integer expectedPages;

    @TableField("completed_pages")
    private Integer completedPages;

    @TableField("success_pages")
    private Integer successPages;

    @TableField("failed_pages")
    private Integer failedPages;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("last_error_code")
    private String lastErrorCode;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("queued_at")
    private LocalDateTime queuedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
