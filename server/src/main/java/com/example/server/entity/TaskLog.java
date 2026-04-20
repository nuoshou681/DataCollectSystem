package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long taskId;
    private Long nodeId;
    private String logMessage;
    private String logLevel;
}
