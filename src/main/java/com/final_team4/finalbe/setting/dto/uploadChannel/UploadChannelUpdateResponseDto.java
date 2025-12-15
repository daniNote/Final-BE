package com.final_team4.finalbe.setting.dto.uploadChannel;

import com.final_team4.finalbe.setting.domain.uploadChannel.Channel;
import com.final_team4.finalbe.setting.domain.uploadChannel.UploadChannel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UploadChannelUpdateResponseDto {
    private Long id;
    private Long userId;
    private Channel name;
    private String apiKey;
    private String clientId;
    private String clientPw;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UploadChannelUpdateResponseDto from(UploadChannel channel) {
        return UploadChannelUpdateResponseDto.builder()
                .id(channel.getId())
                .userId(channel.getUserId())
                .name(channel.getName())
                .apiKey(channel.getApiKey())
                .clientId(channel.getClientId())
                .clientPw(channel.getClientPw())
                .status(channel.getStatus())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }
}
