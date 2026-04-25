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
@TableName("task_batch")
public class TaskBatch {
    @TableId(value = "batch_id", type = IdType.INPUT)
    private String batchId;

    @TableField("batch_name")
    private String batchName;

    @TableField("created_by")
    private Long createdBy;

    @TableField("task_count")
    private Integer taskCount;

    private String status;
    private String notes;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
