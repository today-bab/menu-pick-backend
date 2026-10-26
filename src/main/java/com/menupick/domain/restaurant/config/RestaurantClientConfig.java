package com.menupick.domain.restaurant.config;

import java.net.http.HttpClient;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import io.micrometer.core.instrument.MeterRegistry;

import com.menupick.domain.restaurant.client.CachingRestaurantClient;
import com.menupick.domain.restaurant.client.KakaoRestaurantClient;
import com.menupick.domain.restaurant.client.RestaurantClient;

/**
 * 카카오 클라이언트를 캐시로 감싸 {@link RestaurantClient}로 등록한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(KakaoProperties.class)
public class RestaurantClientConfig {

	@Bean
	RestaurantClient restaurantClient(RestClient.Builder builder, KakaoProperties properties,
			CacheManager cacheManager, MeterRegistry meterRegistry) {
		KakaoRestaurantClient kakao = new KakaoRestaurantClient(kakaoRestClient(builder, properties), properties);
		return new CachingRestaurantClient(kakao,
				Objects.requireNonNull(cacheManager.getCache(CachingRestaurantClient.CACHE_NAME)), meterRegistry);
	}

	private static RestClient kakaoRestClient(RestClient.Builder builder, KakaoProperties properties) {
		HttpClientSettings settings = HttpClientSettings.defaults()
				.withTimeouts(properties.connectTimeout(), properties.readTimeout());
		return builder
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restKey())
				.requestFactory(ClientHttpRequestFactoryBuilder.jdk()
						// 구역들을 동시에 검색하므로 HTTP/2 연결 하나에 몰리면 서버의 동시 스트림 한도를 넘어
						// "too many concurrent streams"로 실패한다. HTTP/1.1이면 필요한 만큼 연결을 늘린다
						.withHttpClientCustomizer(client -> client.version(HttpClient.Version.HTTP_1_1))
						.build(settings))
				.build();
	}

}
