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
@NoArgsConstructor
@AllArgsConstructor
@TableName("export_record")
public class ExportRecord {
    @TableId(value = "export_id", type = IdType.AUTO)
    private Long exportId;

    @TableField("user_id")
    private Long userId;

    @TableField("task_id")
    private Long taskId;

    @TableField("export_scope")
    private String exportScope;

    @TableField("export_type")
    private String exportType;

    @TableField("file_name")
    private String fileName;

    @TableField("record_count")
    private Integer recordCount;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
