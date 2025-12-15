package com.final_team4.finalbe.uploadChannel.service;

import com.final_team4.finalbe._core.exception.ContentNotFoundException;
import com.final_team4.finalbe.setting.domain.uploadChannel.Channel;
import com.final_team4.finalbe.setting.domain.uploadChannel.UploadChannel;
import com.final_team4.finalbe.setting.dto.uploadChannel.UploadChannelItemPayloadDto;
import com.final_team4.finalbe.setting.mapper.uploadChannel.UploadChannelMapper;
import com.final_team4.finalbe.setting.service.uploadChannel.UploadChannelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UploadChannelServiceTest {

    @Mock
    UploadChannelMapper uploadChannelMapper;

    @InjectMocks
    UploadChannelService uploadChannelService;

    @DisplayName("userId가 null이면 IllegalArgumentException이 발생한다")
    @Test
    void getChannelsByUserId_nullUser() {
        assertThatThrownBy(() -> uploadChannelService.getChannelsByUserId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유저 아이디");
    }

    @DisplayName("채널을 찾지 못하면 ContentNotFoundException을 던진다")
    @Test
    void getChannelsByUserId_emptyResult() {
        // given
        given(uploadChannelMapper.findByUserId(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> uploadChannelService.getChannelsByUserId(1L))
                .isInstanceOf(ContentNotFoundException.class)
                .hasMessageContaining("업로드 채널");
    }

    @DisplayName("채널 목록을 조회하면 DTO 리스트를 반환한다")
    @Test
    void getChannelsByUserId_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        UploadChannel channel1 = UploadChannel.builder()
                .id(10L)
                .userId(1L)
                .name(Channel.X)
                .apiKey("api-key-1")
                .status(true)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();
        UploadChannel channel2 = UploadChannel.builder()
                .id(11L)
                .userId(1L)
                .name(Channel.INSTAGRAM)
                .apiKey("api-key-2")
                .status(false)
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusHours(1))
                .build();
        given(uploadChannelMapper.findByUserId(1L)).willReturn(List.of(channel1, channel2));

        // when
        List<UploadChannelItemPayloadDto> result = uploadChannelService.getChannelsByUserId(1L);

        // then
        assertThat(result)
                .hasSize(2)
                .extracting(UploadChannelItemPayloadDto::getId)
                .containsExactly(10L, 11L);
        assertThat(result.get(0).getName()).isEqualTo(Channel.X);
        assertThat(result.get(1).getStatus()).isFalse();
    }
}
