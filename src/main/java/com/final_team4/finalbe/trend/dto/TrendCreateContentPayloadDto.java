package com.final_team4.finalbe.trend.dto;

import com.final_team4.finalbe.setting.dto.llm.LlmChannelDetailResponseDto;
import com.final_team4.finalbe.setting.dto.uploadChannel.UploadChannelItemPayloadDto;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrendCreateContentPayloadDto {
    @NotNull
    private final Long userId;

    @NotBlank
    private final String keyword;

    @NotEmpty
    private final List<UploadChannelItemPayloadDto> uploadChannels;

    @NotNull
    private final UUID jobId;

    @NotNull
    private final LlmChannelDetailResponseDto llmChannel;

    public static TrendCreateContentPayloadDto of(Long userId,
                                                  String keyword,
                                                  List<UploadChannelItemPayloadDto> uploadChannels,
                                                  UUID jobId,
                                                  LlmChannelDetailResponseDto llmChannel) {
        return TrendCreateContentPayloadDto.builder()
                .userId(userId)
                .keyword(keyword)
                .uploadChannels(uploadChannels)
                .jobId(jobId)
                .llmChannel(llmChannel)
                .build();
    }
}
