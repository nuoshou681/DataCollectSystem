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
@TableName("result_tag")
public class ResultTag {
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    @TableField("user_id")
    private Long userId;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_color")
    private String tagColor;

    @TableField("category_name")
    private String categoryName;

    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
