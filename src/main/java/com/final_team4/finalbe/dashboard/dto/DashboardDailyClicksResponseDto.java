package com.final_team4.finalbe.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardDailyClicksResponseDto {
    private String start;
    private String end;
    private List<DailyClicksDto> dailyClicks;
    private long contentsCount;
}
