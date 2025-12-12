package com.final_team4.finalbe.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DashboardContentItemDto {
    private Long contentId;
    private String title;
    private String keyword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String link;
    private long clickCount;
    private Long categoryId;
    private String categoryName;

    public static DashboardContentItemDto from(DashboardContentSummary summary) {
        return DashboardContentItemDto.builder()
                .contentId(summary.getContentId())
                .title(summary.getTitle())
                .keyword(summary.getKeyword())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .link(summary.getLink())
                .clickCount(summary.getClickCount())
                .categoryId(summary.getCategoryId())
                .categoryName(summary.getCategoryName())
                .build();
    }

}
