package com.example.server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String clientName;
    private String status;
    private LocalDateTime lastHeartbeat;

}
