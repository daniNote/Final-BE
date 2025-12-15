package com.final_team4.finalbe.setting.controller.uploadChannel;

import com.final_team4.finalbe._core.security.JwtPrincipal;
import com.final_team4.finalbe.setting.dto.uploadChannel.UploadChannelItemPayloadDto;
import com.final_team4.finalbe.setting.dto.uploadChannel.UploadChannelUpdateRequestDto;
import com.final_team4.finalbe.setting.dto.uploadChannel.UploadChannelUpdateResponseDto;
import com.final_team4.finalbe.setting.service.uploadChannel.UploadChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/setting/uploadChannel")
public class UploadChannelController {

    private final UploadChannelService uploadChannelService;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UploadChannelItemPayloadDto getChannelById(@AuthenticationPrincipal JwtPrincipal user, @PathVariable Long id) {
        return uploadChannelService.getChannelById(user.userId(), id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UploadChannelItemPayloadDto> getChannelsByUserId(@AuthenticationPrincipal JwtPrincipal user) {
        return uploadChannelService.getChannelsByUserId(user.userId());
    }

    @PutMapping("/{id}")
    public UploadChannelUpdateResponseDto updateChannel(@AuthenticationPrincipal JwtPrincipal user, @PathVariable Long id, @RequestBody UploadChannelUpdateRequestDto requestDto) {
        return uploadChannelService.updateChannel(user.userId(), id, requestDto);
    }

    @PutMapping("/{id}/active")
    public UploadChannelItemPayloadDto updateStatus(@AuthenticationPrincipal JwtPrincipal user, @PathVariable Long id) {
        return uploadChannelService.updateStatus(user.userId(), id);
    }
}
