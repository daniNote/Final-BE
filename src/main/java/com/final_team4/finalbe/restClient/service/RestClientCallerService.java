package com.final_team4.finalbe.restClient.service;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 다른 서버에 요청을 보내는 작업을 담당하는 서비스입니다.
 * TODO: 클래스 자체에서 파이썬 서버로 어떠한 요청을 보냈다고 로깅할 수 있는 시스템이 필요합니다.
 */
@Service
@RequiredArgsConstructor
public class RestClientCallerService{
  private final RestClient restClient;

  // 키워드 생성해달라고 요청
  public boolean callGetKeywords() {
    ResponseEntity<Void> result = restClient.get()
            .uri("/api/crawler")
            .retrieve()
            .toBodilessEntity();

    return result.getStatusCode().is2xxSuccessful();
  }

  // 직접 uri를 정하고 싶을 경우 사용하는 메서드입니다.
  // 유지보수를 위해 callGeneratePosts와 callUploadPosts를 추상화한 것 뿐입니다.
  public ResponseEntity<Void> callPost(Object requestData, String uri) {
    return restClient.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestData)
            .retrieve()
            .toBodilessEntity();
  }

  // 글 생성해달라고 요청
  public boolean callGeneratePosts(Object requestData) {
    ResponseEntity<Void> result = callPost(requestData, "/api/write");

    return result.getStatusCode().is2xxSuccessful();
  }

  // 글 업로드 해달라고 요청
  public boolean callUploadPosts(Object requestData) {
    ResponseEntity<Void> result = callPost(requestData, "/api/upload");

    return result.getStatusCode().is2xxSuccessful();
  }
}
