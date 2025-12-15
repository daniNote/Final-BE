package com.final_team4.finalbe.auth.controller;

import com.final_team4.finalbe._core.config.GlobalExceptionHandler;
import com.final_team4.finalbe._core.exception.UnauthorizedException;
import com.final_team4.finalbe._core.jwt.JwtToken;
import com.final_team4.finalbe._core.security.AccessCookieManager;
import com.final_team4.finalbe.auth.dto.request.LoginRequest;
import com.final_team4.finalbe.auth.dto.response.LoginResponse;
import com.final_team4.finalbe.auth.service.AuthService;
import com.final_team4.finalbe._core.jwt.JwtTokenService;
import com.final_team4.finalbe.content.mapper.ContentMapper;
import com.final_team4.finalbe.dashboard.mapper.ClicksMapper;
import com.final_team4.finalbe.dashboard.mapper.DashboardMapper;
import com.final_team4.finalbe.link.mapper.LinkMapper;
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
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
        "security.jwt.secret=dGVzdGluZy1zZWNyZXQtc3RyaW5nLWJhc2U2NA==",
        "security.jwt.issuer=auth-controller-test",
        "security.jwt.temp-validity=PT1H"
})
class AuthControllerTest {

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
    LinkMapper linkMapper;



    @DisplayName("로그인 성공 - AuthService 응답 반환")
    @Test
    void login_success() throws Exception {
        // given
        JwtToken token = new JwtToken(
                "token",
                Instant.parse("2025-11-21T00:00:00Z"),             // issuedAt
                Instant.parse("2025-11-22T00:00:00Z"),             // expiresAt
                10L,                                               // userId
                "tester",                                          // name
                "ROLE_USER"
        );

        LoginResponse loginResponse = new LoginResponse(
                token,
                Instant.parse("2025-11-21T00:00:00Z"),
                Instant.parse("2025-11-22T00:00:00Z"),
                10L,
                "tester",
                "ROLE_USER"
        );
        given(authService.login(any(LoginRequest.class))).willReturn(loginResponse);
        String requestBody = """
                {
                  "email": "tester@example.com",
                  "password": "password123!"
                }
                """;

        // when && then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.name").value("tester"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
        verify(authService).login(any(LoginRequest.class));
    }

    @DisplayName("로그인 실패 - 미등록 이메일 401 반환")
    @Test
    void login_fail_emailNotFound() throws Exception {
        // given
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new UnauthorizedException("가입되지 않은 이메일입니다."));
        String requestBody = """
                {
                  "email": "missing@example.com",
                  "password": "password123!"
                }
                """;

        // when && then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("가입되지 않은 이메일입니다."))
                .andExpect(jsonPath("$.status").value(401));
        verify(authService).login(any(LoginRequest.class));
    }

    @DisplayName("로그인 실패 - 비밀번호 불일치 401 반환")
    @Test
    void login_fail_wrongPassword() throws Exception {
        // given
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new UnauthorizedException("비밀번호가 일치하지 않습니다."));
        String requestBody = """
                {
                  "email": "tester@example.com",
                  "password": "wrong-password"
                }
                """;

        // when && then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("비밀번호가 일치하지 않습니다."))
                .andExpect(jsonPath("$.status").value(401));
        verify(authService).login(any(LoginRequest.class));
    }
}
