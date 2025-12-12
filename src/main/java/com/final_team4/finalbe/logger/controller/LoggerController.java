package com.final_team4.finalbe.logger.controller;

import com.final_team4.finalbe._core.security.JwtPrincipal;
import com.final_team4.finalbe.logger.domain.type.LogType;
import com.final_team4.finalbe.logger.dto.LogResponseDto;
import com.final_team4.finalbe.logger.dto.PipelineLogCreateRequest;
import com.final_team4.finalbe.logger.service.LoggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoggerController {

  private final LoggerService loggerService;

  /**
   * 파이프라인 로그를 생성합니다.
   */
  @PostMapping(value = "/log", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void createLog(@RequestBody PipelineLogCreateRequest request) {
    loggerService.createPipelineLog(request);
  }

  /**
   * 검색어/페이지네이션으로 자신의 로그를 조회합니다.
   */
  @GetMapping("/log")
  public List<LogResponseDto> findLogs(@AuthenticationPrincipal JwtPrincipal principal,
                                       @RequestParam(required = false) String search,
                                       @RequestParam int page,
                                       @RequestParam int size) {
    Long userId = principal.userId();
    return loggerService.findLogs(userId, search, page, size);
  }

  /**
   * 자신의 로그를 LogType별로 집계합니다.
   */
  @GetMapping("/log/count")
  public Map<String, Long> countLogs(@AuthenticationPrincipal JwtPrincipal principal) {
    Map<LogType, Long> counts = loggerService.countLogsByType(principal.userId());
    return Map.of(
        "info", counts.getOrDefault(LogType.INFO, 0L),
        "error", counts.getOrDefault(LogType.ERROR, 0L),
        "warn", counts.getOrDefault(LogType.WARN, 0L)

    );
  }

  /**
   * 특정 jobId의 로그를 SSE로 스트리밍합니다.
   */
  @GetMapping(value = "/pipeline/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> streamLogs(@PathVariable("jobId") String jobId,
                                               @RequestParam(value = "id", required = false) Long fromId,
                                               @AuthenticationPrincipal JwtPrincipal principal,
                                               HttpServletResponse response) {
    response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    SseEmitter emitter = loggerService.streamLogs(jobId, fromId, principal.userId());
    return ResponseEntity.ok()
        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
        .body(emitter);
  }
}
