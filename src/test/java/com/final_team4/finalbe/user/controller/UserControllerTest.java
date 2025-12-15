package com.final_team4.finalbe.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.final_team4.finalbe._core.jwt.JwtToken;
import com.final_team4.finalbe._core.jwt.JwtTokenService;
import com.final_team4.finalbe._core.security.AccessCookieManager;
import com.final_team4.finalbe._core.security.AccessTokenPayload;
import com.final_team4.finalbe._core.security.JwtPrincipal;
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
import com.final_team4.finalbe.user.domain.RoleType;
import com.final_team4.finalbe.user.domain.User;
import com.final_team4.finalbe.user.dto.PasswordUpdateRequest;
import com.final_team4.finalbe.user.dto.UserRegisterRequestDto;
import com.final_team4.finalbe.user.dto.UserUpdateRequest;
import com.final_team4.finalbe.user.dto.response.UserFullResponse;
import com.final_team4.finalbe.user.dto.response.UserSummaryResponse;
import com.final_team4.finalbe.user.mapper.UserInfoMapper;
import com.final_team4.finalbe.user.mapper.UserMapper;
import com.final_team4.finalbe.user.service.UserService;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                MybatisAutoConfiguration.class // 자동으로 MyBatis띄우지 않도록 함
        }
)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    ClicksMapper clicksMapper;

    @MockitoBean
    DashboardMapper dashboardMapper;

    @MockitoBean
    LlmChannelMapper llmChannelMapper;

    @MockitoBean
    NotificationMapper notificationMapper;

    @MockitoBean
    LinkMapper linkMapper;




    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @TestConfiguration
    static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @DisplayName("회원가입 성공 시  유저 요약 정보를 반환한다")
    @Test
    void register_success() throws Exception {
        // given
        UserSummaryResponse summary = UserSummaryResponse.builder()
                .userId(1L)
                .email("codex@example.com")
                .name("codex")
                .role("ROLE_USER")
                .build();
        given(userService.register(any(UserRegisterRequestDto.class))).willReturn(summary);

        UserRegisterRequestDto request = UserRegisterRequestDto.builder()
                .email("codex@example.com")
                .password("pw1234!@")
                .name("codex")
                .build();

        // when & then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("codex@example.com"))
                .andExpect(jsonPath("$.name").value("codex"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        ArgumentCaptor<UserRegisterRequestDto> captor = ArgumentCaptor.forClass(UserRegisterRequestDto.class);
        verify(userService).register(captor.capture());
        UserRegisterRequestDto captured = captor.getValue();
        assertThat(captured.getEmail()).isEqualTo("codex@example.com");
        assertThat(captured.getPassword()).isEqualTo("pw1234!@");
        assertThat(captured.getName()).isEqualTo("codex");
    }

    @DisplayName("회원가입 실패 - 필수 값이 없으면 400 Bad Request를 반환하고 서비스가 호출되지 않는다")
    @Test
    void register_validationError() throws Exception {
        // given
        String invalidPayload = """
                {
                    "email": "not-an-email",
                    "password": "",
                    "name": ""
                }
                """;

        // when & then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @DisplayName("/api/user/me - 로그인된 사용자의 모든 정보를 반환한다")
    @Test
    void me_success() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2024,1,1,0,0);
        LocalDateTime updatedAt = createdAt.plusDays(1);
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        // given
        UserFullResponse profile = UserFullResponse.builder()
                .userId(1L)
                .email("me@example.com")
                .name("me")
                .role("ROLE_USER")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .isDelete(false)
                .build();
        given(userService.findProfile(1L)).willReturn(profile);

        JwtPrincipal principal = new JwtPrincipal(
                1L,
                "me@example.com",
                "me",
                "ROLE_USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,  // accountNonExpired
                true,  // accountNonLocked
                true,  // credentialsNonExpired
                true   // enabled
        );
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // when & then
        mockMvc.perform(get("/api/user/me")
                        .with(authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.name").value("me"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.format(fmt)))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.format(fmt)))
                .andExpect(jsonPath("$.isDelete").value(false));
        verify(userService).findProfile(1L);
    }


    @DisplayName("/api/user/logout - 쿠키를 삭제하고 보안 컨텍스트를 비운다")
    @Test
    void logout_clearsCookiesAndContext() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(
                1L,
                "logout@example.com",
                "logout",
                "ROLE_USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,
                true,
                true,
                true
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());

        //when
         mockMvc.perform(post("/api/user/logout")
                .with(authentication(authenticationToken)))
                .andExpect(status().isNoContent());

        // then
        // AccessCookieManager의 clear 메서드(or addExpiredCookie 등)가 호출됐는지 검증
        // 메서드 이름에 맞게 수정해서 써
        verify(accessCookieManager).clearAccessCookies(any()); // or addExpiredCookie(...)

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

    }

    @DisplayName("이름 수정 시 새 토큰/쿠키를 발급한다")
    @Test
    void update_success() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(
                1L,
                "me@example.com",
                "me",
                "ROLE_USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,true,true,true);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, "oldToken", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        User updated = User.builder()
                .id(1L)
                .roleId(RoleType.USER.getId())
                .email("me@example.com")
                .name("new-Name")
                .build();

        JwtToken newToken = new JwtToken(
                "new.jwt.token",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T01:00:00Z"),
                1L,
                "new-Name",
                "ROLE_USER"
        );

        Authentication refreshedAuth = new UsernamePasswordAuthenticationToken(
                principal, newToken.value(), principal.getAuthorities()
        );

        given(userService.updateProfile(eq(1L), any(UserUpdateRequest.class))).willReturn(updated);
        given(jwtTokenService.issueToken(updated)).willReturn(newToken);
        given(jwtTokenService.authenticate(newToken.value())).willReturn(refreshedAuth);


        mockMvc.perform(patch("/api/user/update")
                        .with(authentication(authenticationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new-Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new-Name"));

        ArgumentCaptor<UserUpdateRequest> captor = ArgumentCaptor.forClass(UserUpdateRequest.class);
        verify(userService).updateProfile(eq(1L), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("new-Name");
        verify(jwtTokenService).issueToken(updated);
        verify(jwtTokenService).authenticate(newToken.value());
        verify(accessCookieManager).clearAccessCookies(any());
        verify(accessCookieManager).setAccessCookies(any(), eq(AccessTokenPayload.from(newToken)));
    }

    @DisplayName("비밀번호 변경 시 서비스 호출 후 쿠키 삭제")
    @Test
    void updatePassword_success() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(
                1L,
                "me@example.com",
                "me", "ROLE_USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,
                true,
                true,
                true
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        mockMvc.perform(patch("/api/user/password")
                        .with(authentication(authenticationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                          {"oldPassword":"oldPass!23","newPassword":"newPass!23","confirmNewPassword":"newPass!23"}
                          """))
                .andExpect(status().isNoContent());


        ArgumentCaptor<PasswordUpdateRequest> captor = ArgumentCaptor.forClass(PasswordUpdateRequest.class);
        verify(userService).updatePassword(captor.capture(), eq(principal.userId()));
        PasswordUpdateRequest captured = captor.getValue();
        assertThat(captured.getOldPassword()).isEqualTo("oldPass!23");
        assertThat(captured.getNewPassword()).isEqualTo("newPass!23");
        assertThat(captured.getConfirmNewPassword()).isEqualTo("newPass!23");
        verify(accessCookieManager).clearAccessCookies(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
