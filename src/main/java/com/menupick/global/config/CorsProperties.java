package com.menupick.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param allowedOrigins API를 호출할 수 있는 프론트엔드 주소
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
