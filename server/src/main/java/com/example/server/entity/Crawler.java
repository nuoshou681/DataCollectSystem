package com.example.server.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Crawler {
    private String nodeId;
    private String status;
    private LocalDateTime lastHeartbeat;

}
