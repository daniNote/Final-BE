package com.final_team4.finalbe.dashboard.controller;

import com.final_team4.finalbe._core.security.JwtPrincipal;
import com.final_team4.finalbe.dashboard.dto.DashboardContentsCountResponseDto;
import com.final_team4.finalbe.dashboard.dto.DashboardContentPageResponseDto;
import com.final_team4.finalbe.dashboard.dto.DashboardDailyClicksResponseDto;
import com.final_team4.finalbe.dashboard.dto.DashboardStatusGetResponseDto;
import com.final_team4.finalbe.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "대시보드 상태 조회",
            description = "전체 클릭수(이것만 구현됨), 조회수, 방문자수, 평균 체류 시간을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/status")
    public DashboardStatusGetResponseDto getStatus
            (@AuthenticationPrincipal JwtPrincipal jwtPrincipal) {

        return dashboardService.getStatus(jwtPrincipal.userId());

    }

    @Operation(
            summary = "대시보드 콘텐츠 전체 조회",
            description = "인증된 사용자가 작성한 모든 콘텐츠 목록을 조회합니다. 콘텐츠 ID, 제목, 키워드, 링크, 클릭 수, 생성/수정 시간을 포함합니다."
            )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/contents")
    public DashboardContentPageResponseDto getContents(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10")int size) {

        return dashboardService.getContents(principal.userId(),page,size);
    }


    @GetMapping("/daily")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "기간별 일별 클릭 수 조회",
            description = "start~end(포함) 기간의 모든 상품 클릭 수를 일별로 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public DashboardDailyClicksResponseDto getDailyClicks(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end

    ) {
        return dashboardService.getDailyClicks(principal.userId(), start, end);
    }


    @GetMapping("/contents/count")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public DashboardContentsCountResponseDto getContentCount(@AuthenticationPrincipal JwtPrincipal principal){

        return dashboardService.countContents(principal.userId());

    }

}
