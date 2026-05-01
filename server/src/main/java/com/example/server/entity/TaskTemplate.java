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
@TableName("task_template")
public class TaskTemplate {
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    @TableField("user_id")
    private Long userId;

    @TableField("template_name")
    private String templateName;

    private String keyword;

    private String url;

    @TableField("site_type")
    private String siteType;

    @TableField("max_links_per_level")
    private Integer maxLinksPerLevel;

    @TableField("tag_ids_json")
    private String tagIdsJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
