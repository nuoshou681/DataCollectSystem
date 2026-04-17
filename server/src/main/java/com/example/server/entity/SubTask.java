package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTask {
    @TableId(type = IdType.AUTO)
    private Long subtaskId;
    private Long taskId;
    // 分配爬虫节点id
    private String nodeId;
    private String url;
    private String keyword;
    @TableField(exist = false)
    private int maxLinksPerLevel = 10;
}
