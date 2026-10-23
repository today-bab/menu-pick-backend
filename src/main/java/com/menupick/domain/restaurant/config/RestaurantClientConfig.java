package com.menupick.domain.restaurant.config;

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
			CacheManager cacheManager) {
		KakaoRestaurantClient kakao = new KakaoRestaurantClient(kakaoRestClient(builder, properties), properties);
		return new CachingRestaurantClient(kakao,
				Objects.requireNonNull(cacheManager.getCache(CachingRestaurantClient.CACHE_NAME)));
	}

	private static RestClient kakaoRestClient(RestClient.Builder builder, KakaoProperties properties) {
		HttpClientSettings settings = HttpClientSettings.defaults()
				.withTimeouts(properties.connectTimeout(), properties.readTimeout());
		return builder
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restKey())
				.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
				.build();
	}

}
