package com.menupick.domain.restaurant.config;

import java.util.List;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.menupick.domain.restaurant.client.CachingRestaurantClient;
import com.menupick.domain.restaurant.model.Restaurant;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * 근처 음식점 캐시 값을 JSON으로 저장한다.
 * <p>
 * 기본 JDK 직렬화는 {@code Serializable}을 요구하고 사람이 읽을 수 없으므로,
 * {@code List<Restaurant>} 타입을 정해 둔 JSON 직렬화로 바꾼다.
 * TTL 등 나머지 설정은 {@code spring.cache.redis.*} 값을 그대로 쓴다.
 */
@Configuration(proxyBeanMethods = false)
public class RestaurantCacheConfig {

	@Bean
	RedisCacheManagerBuilderCustomizer restaurantCacheSerialization(JsonMapper jsonMapper) {
		SerializationPair<List<Restaurant>> values = SerializationPair.fromSerializer(restaurantsSerializer(jsonMapper));
		return builder -> builder.getCacheConfigurationFor(CachingRestaurantClient.CACHE_NAME)
				.map(config -> config.serializeValuesWith(values))
				.ifPresent(config -> builder.withCacheConfiguration(CachingRestaurantClient.CACHE_NAME, config));
	}

	private static JacksonJsonRedisSerializer<List<Restaurant>> restaurantsSerializer(JsonMapper jsonMapper) {
		JavaType type = jsonMapper.getTypeFactory().constructCollectionType(List.class, Restaurant.class);
		return new JacksonJsonRedisSerializer<>(jsonMapper, type);
	}

}
