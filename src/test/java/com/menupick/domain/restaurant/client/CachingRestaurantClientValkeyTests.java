package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 실제 Valkey 컨테이너에 캐시가 저장되고 다시 읽히는지 확인한다. Docker가 없으면 건너뛴다.
 */
@SpringBootTest(properties = "kakao.rest-key=test-key")
@Testcontainers(disabledWithoutDocker = true)
class CachingRestaurantClientValkeyTests {

	@Container
	@ServiceConnection(name = "redis")
	static final GenericContainer<?> valkey = new GenericContainer<>("valkey/valkey:9.1.2-alpine")
			.withExposedPorts(6379);

	private static final Restaurant PYONGYANG = new Restaurant("평양면옥", "냉면", 100,
			new Coordinate(37.567, 126.979), "http://place.map.kakao.com/1");

	private static final String CELL_KEY = CachingRestaurantClient.CACHE_NAME + "::"
			+ new Coordinate(37.567, 126.978);

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private StringRedisTemplate redis;

	private final List<Coordinate> requested = new ArrayList<>();

	private CachingRestaurantClient client;

	@BeforeEach
	void setUp() {
		redis.delete(CELL_KEY);
		RestaurantClient delegate = coordinate -> {
			requested.add(coordinate);
			return List.of(PYONGYANG);
		};
		client = new CachingRestaurantClient(delegate,
				Objects.requireNonNull(cacheManager.getCache(CachingRestaurantClient.CACHE_NAME)));
	}

	@Test
	void 같은_칸은_Valkey에_저장된_목록으로_응답한다() {
		client.findNearby(new Coordinate(37.56661, 126.97801));
		// 저장은 비동기로 끝날 수 있다
		await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(redis.hasKey(CELL_KEY)));

		List<Restaurant> second = client.findNearby(new Coordinate(37.56689, 126.97779));

		assertThat(second).containsExactly(PYONGYANG);
		assertThat(requested).hasSize(1);
	}

	@Test
	void JSON으로_10분_동안_저장한다() {
		client.findNearby(new Coordinate(37.5665, 126.978));
		await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(redis.hasKey(CELL_KEY)));

		assertThat(redis.opsForValue().get(CELL_KEY)).contains("\"name\":\"평양면옥\"");
		assertThat(redis.getExpire(CELL_KEY)).isBetween(1L, Duration.ofMinutes(10).toSeconds());
	}

}
