package com.final_team4.finalbe.logger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.final_team4.finalbe._core.security.JwtPrincipal;
import com.final_team4.finalbe.logger.domain.type.LogType;
import com.final_team4.finalbe.logger.dto.LogResponseDto;
import com.final_team4.finalbe.logger.dto.PipelineLogCreateRequest;
import com.final_team4.finalbe.logger.service.LoggerService;
import com.final_team4.finalbe.content.mapper.ContentMapper;
import com.final_team4.finalbe.schedule.mapper.ScheduleMapper;
import com.final_team4.finalbe.schedule.mapper.ScheduleSettingMapper;
import com.final_team4.finalbe.trend.mapper.TrendMapper;
import com.final_team4.finalbe.setting.mapper.uploadChannel.UploadChannelMapper;
import com.final_team4.finalbe.user.mapper.UserInfoMapper;
import com.final_team4.finalbe.user.mapper.UserMapper;
import com.final_team4.finalbe._core.security.AccessCookieManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoggerController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, MybatisAutoConfiguration.class})
public class LoggerControllerTest {
  @Autowired
  MockMvc mockMvc;

  @Autowired
  ObjectMapper objectMapper;

  @MockitoBean
  LoggerService loggerService;

  @MockitoBean
  ContentMapper contentMapper;

  @MockitoBean
  com.final_team4.finalbe.logger.aop.Loggable loggable;

  @MockitoBean
  com.final_team4.finalbe.logger.mapper.LoggerMapper loggerMapper;

  @MockitoBean
  ScheduleMapper scheduleMapper;

  @MockitoBean
  ScheduleSettingMapper scheduleSettingMapper;

  @MockitoBean
  TrendMapper trendMapper;

  @MockitoBean
  UploadChannelMapper uploadChannelMapper;

  @MockitoBean
  UserMapper userMapper;

  @MockitoBean
  UserInfoMapper userInfoMapper;

  @MockitoBean
  AccessCookieManager accessCookieManager;

  @TestConfiguration
  static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
      resolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  /*
    엔드포인트: /api/log [POST]
    입력 Body 예시:
      {
        id: 1,
        user_id: 1,
        message: "로그 메세지",
        logType: "INFO",
        loggedProcess: "END",
        loggedDate "2025-11-19T14:32:25.00", << 이건 그냥 LocaldateTime으로 받으면 될거임 ㅇㅇ
        jobId: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
        submessage: "50개 상품 수집, 1개 선택 됨",
      }
    동작: LoggerService.createLog 호출하여 로그 생성
          이때, LogCreateRequestDto.message는 "{loggedProcess} | {loggedDate} | {message} \n\t{submessage}" 형태로 만들어져야 함
    출력: 없음 (200 OK만)
   */
  @DisplayName("/api/log POST - 파이프라인 로그 생성")
  @Test
  void createLog_success() throws Exception {
    // given
    PipelineLogCreateRequest payload = PipelineLogCreateRequest.builder()
        .userId(1L)
        .logType(LogType.INFO)
        .loggedProcess("END")
        .loggedDate(LocalDateTime.of(2025, 11, 19, 14, 32, 25))
        .message("로그 메세지")
        .submessage("50개 상품 수집, 1개 선택 됨")
        .jobId("job-123")
        .build();
    given(loggerService.createPipelineLog(any(PipelineLogCreateRequest.class)))
        .willReturn(LogResponseDto.builder().id(1L).build());

    // when & then
    mockMvc.perform(post("/api/log")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk());

    verify(loggerService).createPipelineLog(any(PipelineLogCreateRequest.class));
  }

  /*
    엔드포인트: /api/log [GET]
    입력 Parameter:
      search: String, 검색어
      page: int, 페이지 번호
      size: int, 페이지 크기
    동작: 전체 로그 페이지네이션하여 검색어에 따라 조회
          user_id가 자신인 것만 조회해야 하며, AuthenticationPrincipal 어노테이션으로 JWT 토큰을 통해 가져올 수 있음.
            자세한 사용 방법은 ScheduleController 에서 확인할 것.
    출력 예시:
      [
        {
          id: 1,
          logType: "INFO",
          message: "로그 메세지",
          submessage: "50개 상품 수집, 1개 선택 됨",
          jobId: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
          loggedDate 2025-11-19T14:32:25.00,
        },
        {
          id: 2,
          logType: "INFO",
          message: "로그 메세지",
          submessage: "50개 상품 수집, 1개 선택 됨"
          jobId: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
   loggedDate "2025-11-19T14:32:25.00",
       },
       ...
      ]
   */
  @DisplayName("/api/log GET - 검색어/페이지로 본인 로그 조회")
  @Test
  void findLogs_success() throws Exception {
    // given
    JwtPrincipal principal = principalOf(1L);
    List<LogResponseDto> responses = List.of(
        LogResponseDto.builder().id(1L).logType(LogType.INFO).message("로그 메세지").jobId("job-111").build(),
        LogResponseDto.builder().id(2L).logType(LogType.ERROR).message("에러 메세지").jobId("job-222").build()
    );
    given(loggerService.findLogs(eq(1L), eq("search"), eq(0), eq(2))).willReturn(responses);
    SecurityContextHolder.getContext().setAuthentication(authToken(principal));

    // when & then
    mockMvc.perform(get("/api/log")
            .param("search", "search")
            .param("page", "0")
            .param("size", "2")
            .with(authentication(authToken(principal))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].logType").value("INFO"))
        .andExpect(jsonPath("$[0].jobId").value("job-111"))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].logType").value("ERROR"))
        .andExpect(jsonPath("$[1].jobId").value("job-222"));

    verify(loggerService).findLogs(1L, "search", 0, 2);
  }

  /*
    엔드포인트: /api/log/count [GET]
    입력: 없음
    동작: 전체 로그에서 LogType 마다의 개수를 셈
          LogType이 바뀜에 따라 같이 추가되어야 함.
          위와 같이 본인 user_id와 같은 것만 세어야 함
    출력 예시:
      {
        info: 5,
        success: 1,
        warning: 1,
        error: 1,
      }
      현재는 info와 error 밖에 없음
   */
  @DisplayName("/api/log/count GET - LogType별 개수 반환")
  @Test
  void countLogs_success() throws Exception {
    // given
    JwtPrincipal principal = principalOf(1L);
    given(loggerService.countLogsByType(1L)).willReturn(Map.of(LogType.INFO, 5L, LogType.ERROR, 1L));
    SecurityContextHolder.getContext().setAuthentication(authToken(principal));

    // when & then
    mockMvc.perform(get("/api/log/count")
            .with(authentication(authToken(principal))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info").value(5))
        .andExpect(jsonPath("$.error").value(1));
  }

  /*
    엔드포인트: /api/pipeline/{job_id} [GET]
    입력 url:
      job_id: String
    입력 Parameter:
      id: int
    동작: 전체 로그에서 job_id에 해당하는 로그를 스트림 형태로 보여줌 (SseEmitor 사용)
          호출 시에는 job_id에 해당하는 전체 로그를 보여주고, 이후 Log에 추가되는대로 업데이트함
            이때 업데이트는 job_id에 해당하는 로그가 createLog 에서 생성된 것이 감지됐을 때마다를 기준으로 함
            만약 id가 제공됐을 경우, 전체 로그를 보여주는게 아닌 해당 id보다 큰 로그를 전체 조회함
            만약 위에서 사용된 AuthenticationPrincipal로 확인된 user_id와 조회된 로그의 user_id가 다를 경우,
              잘못된 접근 예외를 던짐
            만약 log.message가 'END' 로 시작하는 객체가 감지됐거나 연결이 끊겼을 경우 로깅이 끝났다고 간주하고 스트림을 종료함
    출력 예시:
      [
        {
          id: 1,
          message: "로그 메세지",
          logType: "INFO",
          loggedDate 2025-11-19T14:32:25.00,
          jobId: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
          submessage: "SELECTION | 50개 상품 수집, 1개 선택 됨",
        },
        {
          id: 2,
          message: "로그 메세지",
          logType: "INFO",
          loggedDate "2025-11-19T14:32:25.00",
          jobId: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
          submessage: "READING | 자료 수집 중"
        },
        ...
        {
          id: 50,
          message: "로그 메세지",
          logType: "INFO",
          loggedDate "2025-11-19T14:32:25.00",
          jobId: "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
          submessage: "END | 파이프라인 종료"
        },
      ]
   */
  @DisplayName("/api/pipeline/{jobId} GET - SSE 스트림 시작")
  @Test
  void streamLogs_success() throws Exception {
    // given
    JwtPrincipal principal = principalOf(1L);
    given(loggerService.streamLogs("job-abc", null, 1L)).willReturn(new SseEmitter());
    SecurityContextHolder.getContext().setAuthentication(authToken(principal));

    // when
    MvcResult result = mockMvc.perform(get("/api/pipeline/{jobId}", "job-abc")
            .with(authentication(authToken(principal))))
        .andExpect(status().isOk())
        .andReturn();

    // then
    assertThat(result.getResponse().getContentType()).contains("text/event-stream");
    verify(loggerService).streamLogs("job-abc", null, 1L);
  }

  private JwtPrincipal principalOf(Long userId) {
    return JwtPrincipal.builder()
        .userId(userId)
        .email("user" + userId + "@example.com")
        .name("user" + userId)
        .role("ROLE_USER")
        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .enabled(true)
        .build();
  }

  private UsernamePasswordAuthenticationToken authToken(JwtPrincipal principal) {
    return new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());
  }
}
