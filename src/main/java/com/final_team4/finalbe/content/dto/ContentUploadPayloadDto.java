package com.final_team4.finalbe.content.dto;

import com.final_team4.finalbe.content.domain.Content;
import com.final_team4.finalbe.setting.domain.uploadChannel.UploadChannel;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentUploadPayloadDto {

    @NotNull
    private final Long userId;

    @NotNull
    private final String channelName;

    @NotNull
    private final String jobId;

    @NotNull
    private final String title;

    @NotNull
    private final String body;

    private final String keyword;

    public static ContentUploadPayloadDto from(Content content, UploadChannel uploadChannel) {
        return ContentUploadPayloadDto.builder()
                .userId(content.getUserId())
                .channelName(uploadChannel.getName().name())
                .jobId(content.getJobId())
                .title(content.getTitle())
                .body(content.getBody())
                .keyword(content.getKeyword())
                .build();
    }
}
