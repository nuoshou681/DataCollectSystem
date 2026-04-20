package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("task")
public class Task {
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    // 分配爬虫节点id
    private String nodeId;
    private String url;
    private String keyword;

    @TableField(exist = false)
    private int maxLinksPerLevel = 10;
}
