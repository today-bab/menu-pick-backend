package com.menupick.domain.restaurant.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 카카오 로컬 API 설정.
 *
 * @param restKey REST API 키
 * @param baseUrl API 주소
 * @param connectTimeout 연결 타임아웃
 * @param readTimeout 응답 타임아웃
 * @param radiusMeters 검색 반경 (카카오 최대 20km)
 * @param maxPages 조회할 최대 페이지 수 (페이지당 15곳, 카카오 최대 3페이지)
 */
@Validated
@ConfigurationProperties("kakao")
public record KakaoProperties(
		@NotBlank String restKey,
		@NotBlank String baseUrl,
		@NotNull Duration connectTimeout,
		@NotNull Duration readTimeout,
		@Min(1) @Max(20_000) int radiusMeters,
		@Min(1) @Max(3) int maxPages) {
}
