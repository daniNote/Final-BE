package com.final_team4.finalbe.dashboard.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyClicksDto {

    private String clickDate;
    private long clicks;
    private long uploads;
}
