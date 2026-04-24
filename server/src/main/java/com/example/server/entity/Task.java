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

    // 分配爬虫节点id
    private String nodeId;
    private String url;
    private String keyword;

    @TableField("task_status")
    private String taskStatus;

    @TableField("task_progress")
    private Integer taskProgress;

    @TableField("total_pages")
    private Integer totalPages;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private int maxLinksPerLevel = 10;
}
