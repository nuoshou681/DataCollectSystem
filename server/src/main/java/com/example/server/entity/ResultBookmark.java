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
@TableName("result_bookmark")
public class ResultBookmark {
    @TableId(value = "bookmark_id", type = IdType.AUTO)
    private Long bookmarkId;

    @TableField("user_id")
    private Long userId;

    @TableField("page_result_id")
    private Long pageResultId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
