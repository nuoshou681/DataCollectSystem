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
@TableName("task_event")
public class TaskEvent {
    @TableId(value = "event_id", type = IdType.AUTO)
    private Long eventId;

    @TableField("task_id")
    private Long taskId;

    @TableField("node_id")
    private String nodeId;

    @TableField("event_type")
    private String eventType;

    @TableField("event_level")
    private String eventLevel;

    @TableField("event_message")
    private String eventMessage;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
