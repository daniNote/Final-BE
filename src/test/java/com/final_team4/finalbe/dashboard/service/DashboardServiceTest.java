package com.final_team4.finalbe.dashboard.service;

import com.final_team4.finalbe.dashboard.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import com.final_team4.finalbe._core.exception.BadRequestException;

import java.time.LocalDate;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
public class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("전체 클릭 수를 반환한다")
    @Test
    void getStatus_returnsClickCount() {
        Long userId = insertUser("dashboard-user");
        Long categoryId = findAnyCategoryId();
        Long uploadChannelId = insertUploadChannel(userId, "main-channel");
        Long contentId = insertContent(
                userId, "keyword", uploadChannelId, "제목", "본문",
                "http://example.com/1", "job-1", "DONE", "AUTO", LocalDateTime.now()
        );
        Long productId = insertProduct(categoryId);
        linkProductToContent(productId, contentId);

        insertClick(productId, "127.0.0.1");
        insertClick(productId, "127.0.0.2");

        DashboardStatusGetResponseDto result = dashboardService.getStatus(userId);

        assertThat(result.getAllClicks()).isEqualTo(2);
        assertThat(result.getAllViews()).isZero();
        assertThat(result.getVisitors()).isZero();
        assertThat(result.getAverageDwellTime()).isZero();
    }

    @DisplayName("사용자별 콘텐츠 목록과 클릭수를 반환한다")
    @Test
    void getContents_returnsSummariesForUser() {
        Long userId = insertUser("dashboard-user");
        Long otherUserId = insertUser("other-user");
        Long categoryId = findAnyCategoryId();
        String keyword = "키워드";
        Long uploadChannelId = insertUploadChannel(userId, "main-channel");

        LocalDateTime older = LocalDateTime.now().minusDays(1);
        Long firstContentId = insertContent(
                userId, keyword , uploadChannelId, "첫번째 콘텐츠", "본문",
                "http://example.com/1", "job-1", "DONE", "AUTO", older
        );
        Long firstProductId = insertProduct(categoryId);
        linkProductToContent(firstProductId, firstContentId);
        insertClick(firstProductId, "10.0.0.1");
        insertClick(firstProductId, "10.0.0.2");

        Long secondContentId = insertContent(
                userId, keyword, uploadChannelId, "두번째 콘텐츠", "본문2",
                "http://example.com/2", "job-2", "DONE", "AUTO", LocalDateTime.now()
        );
        Long secondProductId = insertProduct(categoryId);
        linkProductToContent(secondProductId, secondContentId);
        insertClick(secondProductId, "10.0.0.3");

        Long otherUploadChannelId = insertUploadChannel(otherUserId, "other-channel");
        String otherKeyword = "다른 키원드";
        Long otherContentId = insertContent(
                otherUserId, otherKeyword, otherUploadChannelId, "다른 유저 콘텐츠", "본문3",
                "http://example.com/3", "job-3", "DONE", "AUTO", LocalDateTime.now()
        );
        Long otherProductId = insertProduct(categoryId);
        linkProductToContent(otherProductId, otherContentId);
        insertClick(otherProductId, "10.0.0.4");

        DashboardContentPageResponseDto response = dashboardService.getContents(userId, 0, 10);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems())
                .extracting(DashboardContentItemDto::getContentId)
                .containsExactly(secondContentId, firstContentId);
        assertThat(response.getItems())
                .extracting(DashboardContentItemDto::getTitle)
                .containsExactly("두번째 콘텐츠", "첫번째 콘텐츠");
        assertThat(response.getItems())
                .extracting(DashboardContentItemDto::getClickCount)
                .containsExactly(1L, 2L);
        assertThat(response.getTotalCount()).isEqualTo(2L);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
    }

    @DisplayName("일자별 클릭 수를 반환한다")
    @Test
    void getDailyClicks_returnsClicksPerDate() {
        Long userId = insertUser("daily-user");
        Long categoryId = findAnyCategoryId();
        Long uploadChannelId = insertUploadChannel(userId, "daily-channel");
        Long contentId = insertContent(
                userId, "키워드", uploadChannelId, "daily title", "body",
                "http://example.com/daily", "job-daily", "DONE", "AUTO", LocalDateTime.now()
        );
        Long productId = insertProduct(categoryId);
        linkProductToContent(productId, contentId);

        insertClick(productId, "10.0.0.1", LocalDate.of(2025, 1, 1));
        insertClick(productId, "10.0.0.2", LocalDate.of(2025, 1, 1));
        insertClick(productId, "10.0.0.3", LocalDate.of(2025, 1, 3));

        DashboardDailyClicksResponseDto response = dashboardService.getDailyClicks(
                userId,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 3)
        );

        assertThat(response.getDailyClicks())
                .extracting(DailyClicksDto::getClicks)
                .containsExactly(2L, 0L, 1L);
    }

    @DisplayName("end가 start보다 빠르면 예외를 던진다")
    @Test
    void getDailyClicks_endBeforeStart_throws() {
        Long userId = insertUser("bad-range");
        assertThatThrownBy(() -> dashboardService.getDailyClicks(
                userId,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 1, 9)))
                .isInstanceOf(BadRequestException.class);
    }

    @DisplayName("30일 초과 조회 시 예외를 던진다")
    @Test
    void getDailyClicks_over30days_throws() {
        Long userId = insertUser("over-range");
        assertThatThrownBy(() -> dashboardService.getDailyClicks(
                userId,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 2, 2)))
                .isInstanceOf(BadRequestException.class);
    }


    private Long insertUser(String name) {
        String email = name + "+" + System.nanoTime() + "@example.com";
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM role WHERE name = ?",
                Long.class,
                "MARKETER"
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (role_id, name, email, password, is_delete) VALUES (?, ?, ?, ?, 0)",
                    new String[]{"id"}
            );
            ps.setLong(1, roleId);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, "password");
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private Long insertUploadChannel(Long userId, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO upload_channel (user_id, name, api_key, status) VALUES (?, ?, ?, 1)",
                    new String[]{"id"}
            );
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setString(3, "api-key");
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private Long insertContent(Long userId, String keyword, Long uploadChannelId,
                               String title, String body, String link, String jobId,
                               String status, String generationType, LocalDateTime updatedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO content (user_id, title, body, status, generation_type, created_at, updated_at, job_id, upload_channel_id, link, keyword) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, body);
            ps.setString(4, status);
            ps.setString(5, generationType);
            Timestamp timestamp = Timestamp.valueOf(updatedAt);
            ps.setTimestamp(6, timestamp);
            ps.setTimestamp(7, timestamp);
            ps.setString(8, jobId);
            ps.setLong(9, uploadChannelId);
            ps.setString(10, link);
            ps.setString(11, keyword);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    //product 테이블에 대한 내용이 사전에 필요해서 insert
    private Long insertProduct(Long categoryId) {
        KeyHolder keyHolder = new GeneratedKeyHolder(); // Insert 실행후 DB가 자동생성해주는 PK 값을 받아오기 위한객체
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO product (category_id, name, link, thumbnail, price) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setLong(1, categoryId);
            ps.setString(2, "test-product");
            ps.setString(3, "http://example.com");
            ps.setString(4, null);
            ps.setLong(5, 1000L);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertClick(Long productId, String ip) {
        jdbcTemplate.update(
                "INSERT INTO clicks (product_id, ip) VALUES (?, ?)",
                productId, ip
        );
    }
    private void insertClick(Long productId, String ip, LocalDate date) {
        jdbcTemplate.update(
                "INSERT INTO clicks (product_id, ip, clicked_at) VALUES (?, ?, ?)",
                productId, ip, Timestamp.valueOf(date.atStartOfDay())
        );
    }

    private void linkProductToContent(Long productId, Long contentId) {
        jdbcTemplate.update(
                "INSERT INTO product_content (product_id, content_id) VALUES (?, ?)",
                productId, contentId
        );
    }


    private Long findAnyCategoryId() {
        Long id = jdbcTemplate.query("SELECT id FROM product_category FETCH FIRST 1 ROWS ONLY",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (id != null) {
            return id;
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO product_category (name, description) VALUES (?, ?)",
                    new String[] {"id"});
            ps.setString(1, "test-category");
            ps.setString(2, "for dashboard test");
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @DisplayName("사용자별 콘텐츠 개수를 반환한다")
    @Test
    void countContents_returnsCountPerUser() {
        Long userId = insertUser("count-user");
        Long otherUserId = insertUser("other-count-user");
        Long uploadChannelId = insertUploadChannel(userId, "count-channel");
        Long otherUploadChannelId = insertUploadChannel(otherUserId, "other-channel");

        insertContent(userId, "키워드", uploadChannelId, "title1", "body1",
                "http://example.com/cc1", "job-cc1", "DONE", "AUTO", LocalDateTime.now());
        insertContent(userId, "키워드", uploadChannelId, "title2", "body2",
                "http://example.com/cc2", "job-cc2", "DONE", "AUTO", LocalDateTime.now().plusMinutes(1));
        insertContent(userId, "키워드", uploadChannelId, "title3", "body3",
                "http://example.com/cc3", "job-cc3", "DONE", "AUTO", LocalDateTime.now().plusMinutes(2));

        insertContent(otherUserId, "다른", otherUploadChannelId, "other title", "other body",
                "http://example.com/other", "job-oc1", "DONE", "AUTO", LocalDateTime.now());

        DashboardContentsCountResponseDto response = dashboardService.countContents(userId);

        assertThat(response.getContentsCount()).isEqualTo(3L);
    }



}
