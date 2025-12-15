package com.final_team4.finalbe.content.service;

import com.final_team4.finalbe.content.domain.*;
import com.final_team4.finalbe.content.dto.*;
import com.final_team4.finalbe.content.mapper.ContentMapper;
import com.final_team4.finalbe.content.dto.ContentUploadPayloadDto;
import com.final_team4.finalbe.product.dto.ProductCreateRequestDto;
import com.final_team4.finalbe.product.dto.ProductCreateResponseDto;
import com.final_team4.finalbe.product.mapper.ProductContentMapper;
import com.final_team4.finalbe.product.service.ProductService;
import com.final_team4.finalbe.restClient.service.RestClientCallerService;
import com.final_team4.finalbe.setting.domain.uploadChannel.Channel;
import com.final_team4.finalbe.setting.domain.uploadChannel.UploadChannel;
import com.final_team4.finalbe.setting.mapper.uploadChannel.UploadChannelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    ContentMapper contentMapper;

    @Mock
    UploadChannelMapper uploadChannelMapper;

    @Mock
    ProductService productService;

    @Mock
    ProductContentMapper productContentMapper;

    @Mock
    RestClientCallerService restClientCallerService;

    @InjectMocks
    ContentService contentService;

    @DisplayName("컨텐츠 목록을 페이지로 조회한다")
    @Test
    void getContents() {
        // given
        List<Content> contents = List.of(
                content(1L, "job-1", "title-1"),
                content(2L, "job-2", "title-2")
        );
        given(contentMapper.findAll(eq(1L), isNull(), eq(2), eq(0))).willReturn(contents);
        given(contentMapper.countAll(1L, null)).willReturn(5L);

        // when
        ContentPagedResponseDto response = contentService.getContents(1L, 0, 2, null);

        // then
        assertThat(response.getItems())
                .hasSize(2)
                .extracting(ContentListResponseDto::getTitle)
                .containsExactly("title-1", "title-2");
        assertThat(response.getTotalCount()).isEqualTo(5L);
    }

    @DisplayName("존재하지 않는 컨텐츠 상세 조회 시 예외를 던진다")
    @Test
    void getContentDetail_notFound() {
        given(contentMapper.findById(1L, 9L)).willReturn(null);

        assertThatThrownBy(() -> contentService.getContentDetail(1L, 9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("컨텐츠");
    }

    @DisplayName("컨텐츠를 생성하면 채널 소유권을 검증하고 insert한다")
    @Test
    void createContent_success() {
        // given
        UploadChannel channel = UploadChannel.builder()
                .id(5L)
                .userId(1L)
                .name(Channel.X)
                .apiKey("key")
                .status(true)
                .build();
        given(uploadChannelMapper.findById(5L)).willReturn(channel);

        ContentCreateRequestDto request = ContentCreateRequestDto.builder()
                .jobId("job-123")
                .uploadChannelId(5L)
                .userId(1L)
                .title("title")
                .body("body")
                .status(ContentStatus.APPROVED)
                .generationType(ContentGenType.AUTO)
                .link("https://example.com/content/1")  // 추가
                .keyword("키워드")                            // 추가
                .product(productRequest())
                .build();

        givenInsertSetsId(10L);
        ProductCreateResponseDto productResponse = ProductCreateResponseDto.builder()
                .id(77L)
                .build();
        given(productService.create(any(ProductCreateRequestDto.class))).willReturn(productResponse);

        // when
        ContentCreateResponseDto response = contentService.createContent(request);

        // then
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        assertThat(response.getGenerationType()).isEqualTo(ContentGenType.MANUAL);
        verify(contentMapper).insert(any(Content.class));
        verify(productService).create(any(ProductCreateRequestDto.class));
        verify(productContentMapper).insert(77L, 10L);
    }

    @DisplayName("존재하지 않는 채널로 생성 시 예외")
    @Test
    void createContent_channelNotFound() {
        given(uploadChannelMapper.findById(1L)).willReturn(null);

        ContentCreateRequestDto request = ContentCreateRequestDto.builder()
                .jobId("job")
                .uploadChannelId(1L)
                .userId(1L)
                .title("title")
                .body("body")
                .status(ContentStatus.PENDING)
                .generationType(ContentGenType.AUTO)
                .link("https://example.com/content/1")  // 추가
                .keyword("키워드")                            // 추가
                .product(productRequest())
                .build();

        assertThatThrownBy(() -> contentService.createContent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("채널");
    }

    @DisplayName("다른 사용자의 채널이면 생성할 수 없다")
    @Test
    void createContent_channelForbidden() {
        UploadChannel channel = UploadChannel.builder()
                .id(1L)
                .userId(2L)
                .name(Channel.X)
                .build();
        given(uploadChannelMapper.findById(1L)).willReturn(channel);

        ContentCreateRequestDto request = ContentCreateRequestDto.builder()
                .jobId("job")
                .uploadChannelId(1L)
                .userId(1L)
                .title("title")
                .body("body")
                .status(ContentStatus.PENDING)
                .generationType(ContentGenType.AUTO)
                .link("https://example.com/content/1")  // 추가
                .keyword("키워드")                            // 추가
                .product(productRequest())
                .build();

        assertThatThrownBy(() -> contentService.createContent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("권한");
    }

    @DisplayName("컨텐츠 수정 시 내용 업데이트 후 매퍼를 호출한다")
    @Test
    void updateContent_success() {
        Content content = content(3L, "job", "old title");
        given(contentMapper.findById(1L, 3L)).willReturn(content);

        ContentUpdateRequestDto request = ContentUpdateRequestDto.builder()
                .title("new title")
                .body("new body")
                .build();

        ContentUpdateResponseDto response = contentService.updateContent(1L, 3L, request);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).update(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("new title");
        assertThat(response.getBody()).isEqualTo("new body");
    }

    @DisplayName("컨텐츠 상태를 변경한다")
    @Test
    void updateContentStatus_success() {
        Content content = content(4L, "job", "title");
        given(contentMapper.findById(1L, 4L)).willReturn(content);
        UploadChannel channel = UploadChannel.builder()
                .id(content.getUploadChannelId())
                .userId(content.getUserId())
                .name(Channel.X)
                .build();
        given(uploadChannelMapper.findById(content.getUploadChannelId())).willReturn(channel);

        ContentStatusUpdateRequestDto request = ContentStatusUpdateRequestDto.builder()
                .status(ContentStatus.APPROVED)
                .build();

        contentService.updateContentStatus(1L, 4L, request);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateStatus(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ContentStatus.APPROVED);
        ArgumentCaptor<ContentUploadPayloadDto> payloadCaptor = ArgumentCaptor.forClass(ContentUploadPayloadDto.class);
        verify(restClientCallerService).callUploadPosts(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getChannelName()).isEqualTo(Channel.X.name());
    }

    @DisplayName("승인 시 업로드 채널이 없으면 예외가 발생한다")
    @Test
    void updateContentStatus_channelNotFound() {
        Content content = content(7L, "job", "title");
        given(contentMapper.findById(1L, 7L)).willReturn(content);
        given(uploadChannelMapper.findById(content.getUploadChannelId())).willReturn(null);

        ContentStatusUpdateRequestDto request = ContentStatusUpdateRequestDto.builder()
                .status(ContentStatus.APPROVED)
                .build();

        assertThatThrownBy(() -> contentService.updateContentStatus(1L, 7L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("채널");

        verify(restClientCallerService, never()).callUploadPosts(any());
    }

    @DisplayName("jobId로 컨텐츠 링크를 갱신한다")
    @Test
    void updateContentLink_success() {
        Content content = content(5L, "job-5", "title");
        given(contentMapper.findByJobId("job-5")).willReturn(content);

        ContentLinkUpdateRequestDto request = ContentLinkUpdateRequestDto.builder()
                .jobId("job-5")
                .link("https://example.com/new-link")
                .build();

        contentService.updateContentLink(request);

        verify(contentMapper).updateLinkByJobId("job-5", "https://example.com/new-link");
    }

    @DisplayName("존재하지 않는 jobId면 링크 갱신 시 예외가 발생한다")
    @Test
    void updateContentLink_notFound() {
        given(contentMapper.findByJobId("job-404")).willReturn(null);

        ContentLinkUpdateRequestDto request = ContentLinkUpdateRequestDto.builder()
                .jobId("job-404")
                .link("https://example.com/new-link")
                .build();

        assertThatThrownBy(() -> contentService.updateContentLink(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobId");

        verify(contentMapper, never()).updateLinkByJobId(any(), any());
    }

    private void givenInsertSetsId(Long id) {
        org.mockito.Mockito.doAnswer(invocation -> {
            Content argument = invocation.getArgument(0);
            ReflectionTestUtils.setField(argument, "id", id);
            return null;
        }).when(contentMapper).insert(any(Content.class));
    }

    private Content content(Long id, String jobId, String title) {
        LocalDateTime now = LocalDateTime.now();
        return Content.builder()
                .id(id)
                .jobId(jobId)
                .userId(1L)
                .uploadChannelId(1L)
                .title(title)
                .body("body")
                .status(ContentStatus.PENDING)
                .generationType(ContentGenType.MANUAL)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();
    }

    private ContentProductRequestDto productRequest() {
        return ContentProductRequestDto.builder()
                .title("상품 제목")
                .link("https://example.com/product")
                .thumbnail("thumb.jpg")
                .price(12000L)
                .category("digital")
                .build();
    }
}
