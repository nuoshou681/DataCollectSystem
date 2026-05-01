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
@TableName("task_schedule")
public class TaskSchedule {
    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Long scheduleId;

    @TableField("user_id")
    private Long userId;

    @TableField("schedule_name")
    private String scheduleName;

    private String keyword;

    private String url;

    @TableField("site_type")
    private String siteType;

    @TableField("max_links_per_level")
    private Integer maxLinksPerLevel;

    @TableField("cron_expression")
    private String cronExpression;

    private Boolean enabled;

    @TableField("last_run_at")
    private LocalDateTime lastRunAt;

    @TableField("next_run_at")
    private LocalDateTime nextRunAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
