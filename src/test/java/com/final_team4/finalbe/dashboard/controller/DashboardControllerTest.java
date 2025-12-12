package com.final_team4.finalbe.dashboard.controller;


import com.final_team4.finalbe._core.jwt.JwtTokenService;
import com.final_team4.finalbe._core.security.AccessCookieManager;
import com.final_team4.finalbe._core.security.JwtPrincipal;
import com.final_team4.finalbe.content.mapper.ContentMapper;
import com.final_team4.finalbe.dashboard.dto.*;
import com.final_team4.finalbe.dashboard.mapper.ClicksMapper;
import com.final_team4.finalbe.dashboard.mapper.DashboardMapper;
import com.final_team4.finalbe.dashboard.service.DashboardService;
import com.final_team4.finalbe.link.mapper.LinkMapper;
import com.final_team4.finalbe.logger.aop.Loggable;
import com.final_team4.finalbe.logger.mapper.LoggerMapper;
import com.final_team4.finalbe.notification.mapper.NotificationMapper;
import com.final_team4.finalbe.product.mapper.ProductCategoryMapper;
import com.final_team4.finalbe.product.mapper.ProductContentMapper;
import com.final_team4.finalbe.product.mapper.ProductMapper;
import com.final_team4.finalbe.schedule.mapper.ScheduleMapper;
import com.final_team4.finalbe.schedule.mapper.ScheduleSettingMapper;
import com.final_team4.finalbe.setting.mapper.llm.LlmChannelMapper;
import com.final_team4.finalbe.setting.mapper.notification.NotificationCredentialMapper;
import com.final_team4.finalbe.trend.mapper.TrendMapper;
import com.final_team4.finalbe.uploadChannel.mapper.UploadChannelMapper;
import com.final_team4.finalbe.user.mapper.UserInfoMapper;
import com.final_team4.finalbe.user.mapper.UserMapper;
import com.final_team4.finalbe.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false) // SecurityFilterChain 무시, 필터까지 검증할 거면 제거하고 @WithMockUser 사용
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    ClicksMapper clicksMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    UserMapper userMapper;

    @MockitoBean
    UserInfoMapper userInfoMapper;

    @MockitoBean
    ScheduleMapper scheduleMapper;

    @MockitoBean
    ScheduleSettingMapper scheduleSettingMapper;

    @MockitoBean
    TrendMapper trendMapper;

    @MockitoBean
    UploadChannelMapper uploadChannelMapper;

    @MockitoBean
    Loggable loggable;

    @MockitoBean
    LoggerMapper loggerMapper;

    @MockitoBean
    AccessCookieManager accessCookieManager;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @MockitoBean
    NotificationCredentialMapper notificationCredentialMapper;

    @MockitoBean
    ContentMapper contentMapper;

    @MockitoBean
    DashboardMapper dashboardMapper;

    @MockitoBean
    LlmChannelMapper llmChannelMapper;

    @MockitoBean
    NotificationMapper notificationMapper;

    @MockitoBean
    LinkMapper linkMapper;

    @MockitoBean
    ProductCategoryMapper productCategoryMapper;

    @MockitoBean
    ProductMapper productMapper;

    @MockitoBean
    ProductContentMapper productContentMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("대시보드 상태 조회는 200과 json 숫자 allClicks및 타 숫자들을 반환")
    void getStatus_returnsJsonWithAllClicks() throws Exception {
        JwtPrincipal principal = testPrincipal(17L);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        DashboardStatusGetResponseDto response = DashboardStatusGetResponseDto.builder()
                .allClicks(123L)
                .allViews(0L)
                .visitors(0L)
                .averageDwellTime(0L)
                .build();

        given(dashboardService.getStatus(principal.userId())).willReturn(response);

        mockMvc.perform(get("/api/dashboard/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allClicks").isNumber())
                .andExpect(jsonPath("$.allClicks").value(123));

        then(dashboardService).should().getStatus(principal.userId());

    }
    @Test
    @DisplayName("대시보드 콘텐츠 전체 조회는 인증 사용자 id로 서비스 호출하고 콘텐츠 목록을 반환")
    void getContents_returnsContentsForAuthenticatedUser() throws Exception {
        JwtPrincipal principal = testPrincipal(17L);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        DashboardContentItemDto item = DashboardContentItemDto.builder()
                .contentId(10L)
                .title("테스트 콘텐츠")
                .keyword("키워드")
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 2, 12, 0))
                .link("http://example.com/content")
                .clickCount(5L)
                .build();

        DashboardContentPageResponseDto response = DashboardContentPageResponseDto.builder()
                .items(List.of(item))
                .totalCount(1L)
                .page(0)
                .size(10)
                .build();

        given(dashboardService.getContents(principal.userId(), 0, 10)).willReturn(response);

        mockMvc.perform(get("/api/dashboard/contents")
                        .principal(authentication)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].contentId").value(item.getContentId()))
                .andExpect(jsonPath("$.items[0].title").value(item.getTitle()))
                .andExpect(jsonPath("$.items[0].clickCount").value(item.getClickCount()))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        then(dashboardService).should().getContents(principal.userId(), 0, 10);
    }

    private JwtPrincipal testPrincipal(Long userId) {
        return JwtPrincipal.builder()
                .userId(userId)
                .email("user" + userId + "@example.com")
                .name("tester")
                .role("MARKETER")
                .authorities(Collections.emptyList())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("/daily 정상 조회 시 기간/일자별 클릭 수 반환")
    void daily_returnsClicksByDate() throws Exception {
        JwtPrincipal principal = testPrincipal(1L);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        DashboardDailyClicksResponseDto response = DashboardDailyClicksResponseDto.builder()
                .start("2025-01-01")
                .end("2025-01-03")
                .dailyClicks(List.of(
                        DailyClicksDto.builder().clickDate("2025-01-01").clicks(2).build(),
                        DailyClicksDto.builder().clickDate("2025-01-02").clicks(0).build(),
                        DailyClicksDto.builder().clickDate("2025-01-03").clicks(1).build()
                ))
                .build();

        given(dashboardService.getDailyClicks(
                principal.userId(),
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-01-03"))
        ).willReturn(response);

        mockMvc.perform(get("/api/dashboard/daily")
                        .principal(auth)
                        .param("start", "2025-01-01")
                        .param("end", "2025-01-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value("2025-01-01"))
                .andExpect(jsonPath("$.end").value("2025-01-03"))
                .andExpect(jsonPath("$.dailyClicks[0].clicks").value(2))
                .andExpect(jsonPath("$.dailyClicks[1].clicks").value(0))
                .andExpect(jsonPath("$.dailyClicks[2].clicks").value(1));
    }


    @Test
    @DisplayName("콘텐츠 개수 조회는 인증 사용자 id로 서비스 호출하고 개수를 반환")
    void getContentCount_returnsCountForAuthenticatedUser() throws Exception {
        JwtPrincipal principal = testPrincipal(5L);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        DashboardContentsCountResponseDto response = DashboardContentsCountResponseDto.of(4L);
        given(dashboardService.countContents(principal.userId())).willReturn(response);

        mockMvc.perform(get("/api/dashboard/contents/count").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contentsCount").value(4));

        then(dashboardService).should().countContents(principal.userId());
    }


}

