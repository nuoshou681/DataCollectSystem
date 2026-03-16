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
    private Long id;
    private Long taskId;
    private Long subtaskId;
    private Long clientId;
    private String logMessage;
    private String logLevel;
}
