package com.final_team4.finalbe.trend.service;

import com.final_team4.finalbe.content.domain.ContentGenType;
import com.final_team4.finalbe.restClient.service.RestClientCallerService;
import com.final_team4.finalbe.setting.dto.llm.LlmChannelDetailResponseDto;
import com.final_team4.finalbe.setting.service.llm.LlmChannelService;
import com.final_team4.finalbe.trend.dto.TrendCreateContentPayloadDto;
import com.final_team4.finalbe.trend.dto.TrendCreateContentRequestDto;
import com.final_team4.finalbe.trend.dto.TrendCreateContentResponseDto;
import com.final_team4.finalbe.trend.mapper.TrendMapper;
import com.final_team4.finalbe.setting.domain.uploadChannel.Channel;
import com.final_team4.finalbe.setting.dto.uploadChannel.UploadChannelItemPayloadDto;
import com.final_team4.finalbe.setting.service.uploadChannel.UploadChannelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrendServiceRequestCreateContentTest {

    @Mock
    TrendMapper trendMapper;

    @Mock
    UploadChannelService uploadChannelService;

    @Mock
    RestClientCallerService restClientCallerService;

    @Mock
    LlmChannelService llmChannelService;

    @InjectMocks
    TrendService trendService;

    @DisplayName("컨텐츠 생성 요청 시 LLM 채널 전체를 payload에 포함한다")
    @Test
    void requestCreateContent_includesLlmChannel() {
        List<UploadChannelItemPayloadDto> channels = List.of(
                UploadChannelItemPayloadDto.builder()
                        .id(10L)
                        .userId(1L)
                        .name(Channel.INSTAGRAM)
                        .apiKey("api-key")
                        .status(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );
        given(uploadChannelService.getChannelsByUserId(1L)).willReturn(channels);

        LlmChannelDetailResponseDto llmChannel = LlmChannelDetailResponseDto.builder()
                .id(7L)
                .userId(1L)
                .name("openai")
                .modelName("gpt-4o")
                .status(true)
                .maxTokens(8000)
                .temperature(BigDecimal.valueOf(0.8))
                .prompt("prompt")
                .apiKey("****1234")
                .generationType(ContentGenType.AUTO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        given(llmChannelService.findByUserId(1L)).willReturn(llmChannel);

        given(restClientCallerService.callGeneratePosts(any(TrendCreateContentPayloadDto.class))).willReturn(true);

        TrendCreateContentRequestDto request = TrendCreateContentRequestDto.builder()
                .keyword("fashion")
                .build();

        TrendCreateContentResponseDto response = trendService.requestCreateContent(request, 1L);

        assertThat(response.isRequested()).isTrue();
        assertThat(response.getKeyword()).isEqualTo("fashion");

        ArgumentCaptor<TrendCreateContentPayloadDto> payloadCaptor = ArgumentCaptor.forClass(TrendCreateContentPayloadDto.class);
        verify(restClientCallerService).callGeneratePosts(payloadCaptor.capture());

        TrendCreateContentPayloadDto payload = payloadCaptor.getValue();
        assertThat(payload.getLlmChannel()).isEqualTo(llmChannel);
        assertThat(payload.getUploadChannels()).containsExactlyElementsOf(channels);
    }
}
