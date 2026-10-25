package com.menupick.domain.restaurant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.menupick.domain.restaurant.client.CachingRestaurantClient;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

import tools.jackson.databind.json.JsonMapper;

class RestaurantCacheConfigTest {

	private static final List<Restaurant> RESTAURANTS = List.of(
			new Restaurant("평양면옥", "냉면", 100, new Coordinate(37.567, 126.979), "http://place.map.kakao.com/1"));

	private final RedisCacheManagerBuilder builder = RedisCacheManager.builder()
			.withCacheConfiguration(CachingRestaurantClient.CACHE_NAME,
					RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)));

	private RedisCacheConfiguration customized() {
		new RestaurantCacheConfig().restaurantCacheSerialization(JsonMapper.builder().build()).customize(builder);
		return builder.getCacheConfigurationFor(CachingRestaurantClient.CACHE_NAME).orElseThrow();
	}

	@Test
	void 음식점_목록을_JSON으로_저장하고_같은_값으로_읽는다() {
		SerializationPair<Object> values = customized().getValueSerializationPair();

		ByteBuffer written = values.write(RESTAURANTS);
		String json = StandardCharsets.UTF_8.decode(written.duplicate()).toString();

		assertThat(json).contains("\"name\":\"평양면옥\"", "\"menu\":\"냉면\"");
		assertThat(values.read(written)).isEqualTo(RESTAURANTS);
	}

	@Test
	void 설정_파일의_TTL은_그대로_둔다() {
		assertThat(customized().getTtlFunction().getTimeToLive(CachingRestaurantClient.CACHE_NAME, RESTAURANTS))
				.isEqualTo(Duration.ofMinutes(10));
	}

}
