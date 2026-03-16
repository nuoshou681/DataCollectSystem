package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskName;
    private String keyword;
    private String targetUrl;
    private String status;
    private Integer progress;
    
}
