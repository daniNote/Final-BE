package com.final_team4.finalbe.dashboard.service;


import com.final_team4.finalbe.dashboard.dto.*;
import com.final_team4.finalbe.dashboard.mapper.ClicksMapper;
import com.final_team4.finalbe.dashboard.mapper.DashboardMapper;
import com.final_team4.finalbe._core.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ClicksMapper clicksMapper;
    private final DashboardMapper dashboardMapper;


    public DashboardStatusGetResponseDto getStatus(Long  userId) {
        long clicks = clicksMapper.countAllClicksByUserId(userId);

        return DashboardStatusGetResponseDto.builder()
                .allClicks(clicks)
                .allViews(0) // 아직  구현할지 미정이고 추후 구현 가능성 때문에 우선 0으로 넣어둠
                .visitors(0)
                .averageDwellTime(0)
                .build();
    }

    public DashboardContentPageResponseDto getContents(Long userId, int page, int size) {

        int offset = page * size;
        List<DashboardContentItemDto> items = dashboardMapper
                .findContentsByUserId(userId, size, offset).stream()
                .map(DashboardContentItemDto::from)
                .toList();
        long totalCount = dashboardMapper.countAllContentsByUserId(userId);

        return DashboardContentPageResponseDto.of(items, totalCount, page, size);
    }

    public DashboardDailyClicksResponseDto getDailyClicks(Long userId, LocalDate start, LocalDate end) {
        // end가 start보다 빠르면 잘못된 입력
        if (end.isBefore(start)) {
            throw new BadRequestException("end 날짜는 start 날짜보다 빠를 수 없습니다.");
        }

        //조회기간 최대 30일
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > 30) {
            throw new BadRequestException("조회 기간은 최대 30일(시작일 포함 기준)입니다.");
        }



        // DB에서 가져온 일별 클릭 합계를 날짜별로 바로 찾을 수 있게 Map으로 변환
        Map<LocalDate, DailyClicksDto> countsByDate = clicksMapper.findDailyClicks(userId, start, end).stream()
                .collect(Collectors.toMap(
                        dto -> LocalDate.parse(dto.getClickDate()), //DailyClicksDto의 날짜 문자열을 LocalDate 키로 변환
                        Function.identity()));

        List<DailyClicksDto> dailyClicksAndUploads = new ArrayList<>();
        // start부터 end까지 하루씩 증가시키며 빈 날짜는 0으로 채움
        //!day.isAfter(end) ==   day <= end
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            DailyClicksDto found = countsByDate.get(day);
            dailyClicksAndUploads.add(DailyClicksDto.builder()
                    .clickDate(day.toString())
                    .clicks(found != null ? found.getClicks() : 0L)
                    .uploads(found != null ? found.getUploads() : 0L)
                    .build());
        }

        // 응답 DTO 조립
        return DashboardDailyClicksResponseDto.builder()
                .start(start.toString())
                .end(end.toString())
                .dailyClicks(dailyClicksAndUploads)
                .build();
    }

    public DashboardContentsCountResponseDto countContents(Long userId) {
        Long counts = dashboardMapper.countAllContentsByUserId(userId);
        // 유저 id 로 콘텐츠 개수 찾기
        return DashboardContentsCountResponseDto.of(counts);
    }

}
