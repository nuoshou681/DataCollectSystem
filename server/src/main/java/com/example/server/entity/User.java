package com.example.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @TableId(type = IdType.AUTO)
    private Long userId;
    private String username;
    private String password;
    private String email;
    @TableField("role")
    private String role;

    @TableField("status")
    private String status;

    @TableField("last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    @TableField("created_at")
    private java.time.LocalDateTime createdAt;

    @TableField("updated_at")
    private java.time.LocalDateTime updatedAt;
}
