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
@TableName("notification")
public class Notification {
    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;

    @TableField("user_id")
    private Long userId;

    private String type;

    private String title;

    private String content;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
