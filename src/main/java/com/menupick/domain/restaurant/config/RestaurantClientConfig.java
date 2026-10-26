package com.menupick.domain.restaurant.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import io.micrometer.core.instrument.MeterRegistry;

import com.menupick.domain.restaurant.client.CacheLoadLock;
import com.menupick.domain.restaurant.client.CachingRestaurantClient;
import com.menupick.domain.restaurant.client.KakaoRestaurantClient;
import com.menupick.domain.restaurant.client.RestaurantClient;
import com.menupick.domain.restaurant.client.ValkeyCacheLoadLock;

/**
 * 카카오 클라이언트를 캐시로 감싸 {@link RestaurantClient}로 등록한다.
 * 같은 칸을 여러 인스턴스가 동시에 채우지 않도록 Valkey 락을 함께 쓴다.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(KakaoProperties.class)
public class RestaurantClientConfig {

	/** 락을 잡은 쪽이 풀지 못하고 죽어도 이 시간이 지나면 풀린다. 정상 조회(약 0.5초)보다 넉넉히 길게 둔다. */
	private static final Duration LOCK_TTL = Duration.ofSeconds(10);

	/** 다른 인스턴스가 같은 칸을 채우기를 기다리는 최대 시간. 넘으면 직접 조회한다. */
	private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(3);

	private static final Duration LOCK_POLL_INTERVAL = Duration.ofMillis(50);

	@Bean
	RestaurantClient restaurantClient(RestClient.Builder builder, KakaoProperties properties,
			CacheManager cacheManager, StringRedisTemplate redis, MeterRegistry meterRegistry) {
		KakaoRestaurantClient kakao = new KakaoRestaurantClient(kakaoRestClient(builder, properties), properties);
		CacheLoadLock lock = new ValkeyCacheLoadLock(redis, CachingRestaurantClient.CACHE_NAME + ":lock::", LOCK_TTL);
		return new CachingRestaurantClient(kakao,
				Objects.requireNonNull(cacheManager.getCache(CachingRestaurantClient.CACHE_NAME)), lock,
				LOCK_WAIT_TIMEOUT, LOCK_POLL_INTERVAL, meterRegistry);
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
