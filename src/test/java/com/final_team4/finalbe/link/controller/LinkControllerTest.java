package com.final_team4.finalbe.link.controller;

import com.final_team4.finalbe._core.exception.InvalidJobIdException;
import com.final_team4.finalbe._core.jwt.JwtTokenService;
import com.final_team4.finalbe._core.security.AccessCookieManager;
import com.final_team4.finalbe.auth.service.AuthService;
import com.final_team4.finalbe.content.mapper.ContentMapper;
import com.final_team4.finalbe.dashboard.mapper.ClicksMapper;
import com.final_team4.finalbe.dashboard.mapper.DashboardMapper;
import com.final_team4.finalbe.link.dto.LinkResponseDto;
import com.final_team4.finalbe.link.mapper.LinkMapper;
import com.final_team4.finalbe.link.service.LinkService;
import com.final_team4.finalbe.logger.aop.Loggable;
import com.final_team4.finalbe.logger.mapper.LoggerMapper;
import com.final_team4.finalbe.notification.mapper.NotificationMapper;
import com.final_team4.finalbe.schedule.mapper.ScheduleMapper;
import com.final_team4.finalbe.schedule.mapper.ScheduleSettingMapper;
import com.final_team4.finalbe.setting.mapper.llm.LlmChannelMapper;
import com.final_team4.finalbe.setting.mapper.notification.NotificationCredentialMapper;
import com.final_team4.finalbe.trend.mapper.TrendMapper;
import com.final_team4.finalbe.setting.mapper.uploadChannel.UploadChannelMapper;
import com.final_team4.finalbe.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LinkController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                MybatisAutoConfiguration.class
        })
class LinkControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @MockitoBean
    UserMapper userMapper;

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
    NotificationCredentialMapper notificationCredentialMapper;

    @MockitoBean
    ContentMapper contentMapper;

    @MockitoBean
    ClicksMapper clicksMapper;

    @MockitoBean
    DashboardMapper dashboardMapper;

    @MockitoBean
    LlmChannelMapper llmChannelMapper;

    @MockitoBean
    NotificationMapper notificationMapper;

    @MockitoBean
    LinkService linkService;

    @MockitoBean
    LinkMapper linkMapper;

    @DisplayName("jobId로 링크를 조회하면 200과 링크를 반환한다")
    @Test
    void getLink_success() throws Exception {
        given(linkService.resolveLink("job-1", "203.0.113.10"))
                .willReturn(LinkResponseDto.of("http://example.com/link"));

        mockMvc.perform(get("/api/link")
                        .param("jobId", "job-1")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").value("http://example.com/link"));

        verify(linkService).resolveLink("job-1", "203.0.113.10");
    }

    @DisplayName("jobId 파라미터가 없으면 400을 반환한다")
    @Test
    void getLink_missingJobId() throws Exception {
        mockMvc.perform(get("/api/link"))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("존재하지 않는 jobId면 400을 반환한다")
    @Test
    void getLink_unknownJobId() throws Exception {
        given(linkService.resolveLink("missing-job", "198.51.100.5"))
                .willThrow(new InvalidJobIdException("존재하지 않는 jobId입니다."));

        mockMvc.perform(get("/api/link")
                        .param("jobId", "missing-job")
                        .header("X-Forwarded-For", "198.51.100.5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(linkService).resolveLink("missing-job", "198.51.100.5");
    }
}
