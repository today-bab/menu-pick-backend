package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

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
				Objects.requireNonNull(cacheManager.getCache(CachingRestaurantClient.CACHE_NAME)),
				new SimpleMeterRegistry());
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

	@Test
	void 두_인스턴스에_같은_칸_요청이_동시에_몰려도_원본은_한_번만_조회한다() throws Exception {
		String lockPrefix = CachingRestaurantClient.CACHE_NAME + ":lock::";
		redis.delete(lockPrefix + new Coordinate(37.567, 126.978));
		AtomicInteger calls = new AtomicInteger();
		RestaurantClient slowKakao = coordinate -> {
			calls.incrementAndGet();
			try {
				// 카카오 구역 검색 한 번 정도의 시간
				Thread.sleep(300);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return List.of(PYONGYANG);
		};
		// 운영의 Pod 2개처럼 캐시와 락만 Valkey로 함께 쓴다
		List<CachingRestaurantClient> pods = IntStream.range(0, 2)
				.mapToObj(i -> new CachingRestaurantClient(slowKakao,
						Objects.requireNonNull(cacheManager.getCache(CachingRestaurantClient.CACHE_NAME)),
						new ValkeyCacheLoadLock(redis, lockPrefix, Duration.ofSeconds(10)),
						Duration.ofSeconds(3), Duration.ofMillis(20), new SimpleMeterRegistry()))
				.toList();

		List<Future<List<Restaurant>>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < 50; i++) {
				CachingRestaurantClient pod = pods.get(i % 2);
				results.add(executor.submit(() -> pod.findNearby(new Coordinate(37.5665, 126.978))));
			}
		}

		for (Future<List<Restaurant>> result : results) {
			assertThat(result.get()).containsExactly(PYONGYANG);
		}
		assertThat(calls).hasValue(1);
	}

}
