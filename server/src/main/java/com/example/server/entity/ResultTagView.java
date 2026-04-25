package com.example.server.entity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultTagView {
    private Long tagId;
    private String tagName;
    private String tagColor;
    private String categoryName;
    private String description;
    private Integer bindingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> pageResultIds;
}
