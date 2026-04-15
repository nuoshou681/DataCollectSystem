package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    @TableId(type = IdType.AUTO)
    private Long taskId;
    private Long userId;
    private String keyword;
    private String url;
    private String status;
    private Integer progress;
    
}
