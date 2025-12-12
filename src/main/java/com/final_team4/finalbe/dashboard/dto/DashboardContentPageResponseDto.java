package com.final_team4.finalbe.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardContentPageResponseDto {
    private List<DashboardContentItemDto> items;
    private final Long totalCount;
    private final int page;
    private final int size;

    public static DashboardContentPageResponseDto of(
            List<DashboardContentItemDto> items, long totalCount, int page, int size) {
        return DashboardContentPageResponseDto.builder()
                .items(items)
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .build();
    }


}
