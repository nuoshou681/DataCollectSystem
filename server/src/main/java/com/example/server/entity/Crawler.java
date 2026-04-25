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
@TableName("crawler")
public class Crawler {
    @TableId("node_id")
    private String nodeId;

    @TableField("node_name")
    private String nodeName;

    private String status;
    private String version;

    @TableField("capabilities_json")
    private String capabilitiesJson;

    @TableField("tags_json")
    private String tagsJson;

    @TableField("max_concurrency")
    private Integer maxConcurrency;

    @TableField("current_load")
    private Integer currentLoad;

    @TableField("heartbeat_timeout_sec")
    private Integer heartbeatTimeoutSec;

    @TableField("last_online_at")
    private LocalDateTime lastOnlineAt;

    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
