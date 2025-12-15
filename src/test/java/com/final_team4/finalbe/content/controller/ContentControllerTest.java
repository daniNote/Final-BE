package com.final_team4.finalbe.content.controller;

import com.final_team4.finalbe._core.security.JwtPrincipal;
import com.final_team4.finalbe.content.domain.ContentStatus;
import com.final_team4.finalbe.content.dto.*;
import com.final_team4.finalbe.content.service.ContentService;
import com.final_team4.finalbe.content.mapper.ContentMapper;
import com.final_team4.finalbe.dashboard.mapper.ClicksMapper;
import com.final_team4.finalbe.dashboard.mapper.DashboardMapper;
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
import com.final_team4.finalbe.setting.mapper.uploadChannel.UploadChannelMapper;
import com.final_team4.finalbe.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ContentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                MybatisAutoConfiguration.class
        })
class ContentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ContentService contentService;

    // 나머지 의존성은 목으로 대체한다.
    @MockitoBean Loggable loggable;
    @MockitoBean ContentMapper contentMapper;
    @MockitoBean LoggerMapper loggerMapper;
    @MockitoBean ScheduleMapper scheduleMapper;
    @MockitoBean ScheduleSettingMapper scheduleSettingMapper;
    @MockitoBean NotificationCredentialMapper notificationCredentialMapper;
    @MockitoBean TrendMapper trendMapper;
    @MockitoBean UploadChannelMapper uploadChannelMapper;
    @MockitoBean UserMapper userMapper;
    @MockitoBean ClicksMapper clicksMapper;
    @MockitoBean DashboardMapper dashboardMapper;
    @MockitoBean NotificationMapper notificationMapper;
    @MockitoBean LlmChannelMapper llmChannelMapper;
    @MockitoBean ProductContentMapper productContentMapper;
    @MockitoBean ProductMapper productMapper;
    @MockitoBean ProductCategoryMapper productCategoryMapper;
    @MockitoBean LinkMapper linkMapper;

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

    @DisplayName("컨텐츠 목록 조회")
    @Test
    void getContents_success() throws Exception {
        List<ContentListResponseDto> responses = List.of(
                ContentListResponseDto.builder().id(1L).title("first").build(),
                ContentListResponseDto.builder().id(2L).title("second").build()
        );
        ContentPagedResponseDto pagedResponse = ContentPagedResponseDto.of(responses, 15L, 0, 10);
        given(contentService.getContents(1L, 0, 10, null)).willReturn(pagedResponse);

        mockMvc.perform(get("/api/content")
                        .with(authentication(authToken(principalOf(1L))))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("first"))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.totalCount").value(15));

        verify(contentService).getContents(1L, 0, 10, null);
    }

    @DisplayName("컨텐츠 상세 조회")
    @Test
    void getContentDetail_success() throws Exception {
        ContentDetailResponseDto detail = ContentDetailResponseDto.builder()
                .id(5L)
                .title("detail")
                .build();
        given(contentService.getContentDetail(1L, 5L)).willReturn(detail);

        mockMvc.perform(get("/api/content/{id}", 5L)
                        .with(authentication(authToken(principalOf(1L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("detail"));

        verify(contentService).getContentDetail(1L, 5L);
    }

    @DisplayName("컨텐츠 생성")
    @Test
    void createContent_success() throws Exception {
        ContentCreateResponseDto response = ContentCreateResponseDto.builder()
                .id(7L)
                .title("new-content")
                .build();
        given(contentService.createContent(any(ContentCreateRequestDto.class))).willReturn(response);

        String payload = """
                {
                  "jobId": "job-1",
                  "uploadChannelId": 3,
                  "userId": 1,
                  "title": "new-content",
                  "body": "body",
                  "status": "PENDING",
                  "generationType": "AUTO",
                  "link": "https://example.com/content/1",
                  "keyword": "키워드",
                  "product": {
                    "title": "상품 제목",
                    "link": "https://example.com/product",
                    "thumbnail": "thumb.jpg",
                    "price": 12000,
                    "category": "digital"
                  }
                }
                """;

        mockMvc.perform(post("/api/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));

        verify(contentService).createContent(any(ContentCreateRequestDto.class));
    }

    @DisplayName("컨텐츠 링크 업데이트")
    @Test
    void updateContentLink_success() throws Exception {
        String payload = """
                {
                  "jobId": "job-1",
                  "link": "https://example.com/uploaded"
                }
                """;

        mockMvc.perform(patch("/api/content/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(contentService).updateContentLink(any(ContentLinkUpdateRequestDto.class));
    }

    @DisplayName("컨텐츠 링크 업데이트 요청에 필수 값이 비어있으면 400을 응답한다")
    @Test
    void updateContentLink_invalidRequest() throws Exception {
        String payload = """
                {
                  "jobId": "",
                  "link": "https://example.com/uploaded"
                }
                """;

        mockMvc.perform(patch("/api/content/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(contentService, never()).updateContentLink(any(ContentLinkUpdateRequestDto.class));
    }

    @DisplayName("컨텐츠 수정")
    @Test
    void updateContent_success() throws Exception {
        ContentUpdateResponseDto response = ContentUpdateResponseDto.builder()
                .id(8L)
                .title("updated")
                .build();
        given(contentService.updateContent(eq(1L), eq(8L), any(ContentUpdateRequestDto.class))).willReturn(response);

        String payload = """
                {
                  "title": "updated",
                  "body": "body"
                }
                """;

        mockMvc.perform(put("/api/content/{id}", 8L)
                        .with(authentication(authToken(principalOf(1L))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("updated"));

        verify(contentService).updateContent(eq(1L), eq(8L), any(ContentUpdateRequestDto.class));
    }

    @DisplayName("컨텐츠 상태 변경")
    @Test
    void updateContentStatus_success() throws Exception {
        ContentUpdateResponseDto response = ContentUpdateResponseDto.builder()
                .id(9L)
                .status(ContentStatus.APPROVED)
                .build();
        given(contentService.updateContentStatus(eq(1L), eq(9L), any(ContentStatusUpdateRequestDto.class)))
                .willReturn(response);

        String payload = """
                {
                  "status": "APPROVED"
                }
                """;

        mockMvc.perform(patch("/api/content/status/{id}", 9L)
                        .with(authentication(authToken(principalOf(1L))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(contentService).updateContentStatus(eq(1L), eq(9L), any(ContentStatusUpdateRequestDto.class));
    }

    private UserDetails principalOf(Long userId) {
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

    private UsernamePasswordAuthenticationToken authToken(UserDetails principal) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
        return token;
    }
}
