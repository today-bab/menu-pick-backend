package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 캐시가 빈 칸에서 락을 잡은 한 곳만 조회하는지 확인한다.
 */
class CachingRestaurantClientLockTest {

	private static final Restaurant PYONGYANG = new Restaurant("평양면옥", "냉면", 100,
			new Coordinate(37.567, 126.979), "http://place.map.kakao.com/1");

	private static final Coordinate HERE = new Coordinate(37.5665, 126.978);
	/** {@link #HERE}가 속한 칸 (캐시·락 키). */
	private static final Coordinate CELL = new Coordinate(37.567, 126.978);

	/** 메모리 안에서 동작하는 락. */
	private static class InMemoryLock implements CacheLoadLock {

		private final Map<Object, String> held = new ConcurrentHashMap<>();

		@Override
		public @Nullable String tryAcquire(Object key) {
			String token = UUID.randomUUID().toString();
			return held.putIfAbsent(key, token) == null ? token : null;
		}

		@Override
		public void release(Object key, String token) {
			held.remove(key, token);
		}

		boolean isHeld(Object key) {
			return held.containsKey(key);
		}

	}

	private final AtomicInteger calls = new AtomicInteger();
	private final RestaurantClient delegate = coordinate -> {
		calls.incrementAndGet();
		return List.of(PYONGYANG);
	};
	private final ConcurrentMapCache cache = new ConcurrentMapCache(CachingRestaurantClient.CACHE_NAME);
	private final InMemoryLock lock = new InMemoryLock();
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private CachingRestaurantClient client(RestaurantClient delegate, CacheLoadLock lock, Duration waitTimeout) {
		return new CachingRestaurantClient(delegate, cache, lock, waitTimeout, Duration.ofMillis(10), meterRegistry);
	}

	/** {@code delayMillis} 뒤에 다른 곳에서 할 일을 실행한다. */
	private static void later(long delayMillis, Runnable action) {
		Thread.ofVirtual().start(() -> {
			try {
				Thread.sleep(delayMillis);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			action.run();
		});
	}

	@Test
	void 락을_잡으면_조회하고_만료될_때까지_락을_둔다() {
		List<Restaurant> found = client(delegate, lock, Duration.ofSeconds(1)).findNearby(HERE);

		assertThat(found).containsExactly(PYONGYANG);
		assertThat(calls).hasValue(1);
		// 저장이 비동기로 끝나기 전에 다른 곳이 락을 잡지 않도록 풀지 않는다
		assertThat(lock.isHeld(CELL)).isTrue();
	}

	@Test
	void 다른_곳이_채우는_중이면_기다렸다가_채운_값을_쓴다() {
		lock.tryAcquire(CELL);
		later(100, () -> cache.put(CELL, List.of(PYONGYANG)));

		List<Restaurant> found = client(delegate, lock, Duration.ofSeconds(2)).findNearby(HERE);

		assertThat(found).containsExactly(PYONGYANG);
		assertThat(calls).hasValue(0);
	}

	@Test
	void 기다려도_채워지지_않으면_직접_조회한다() {
		lock.tryAcquire(CELL);

		List<Restaurant> found = client(delegate, lock, Duration.ofMillis(100)).findNearby(HERE);

		assertThat(found).containsExactly(PYONGYANG);
		assertThat(calls).hasValue(1);
	}

	@Test
	void 먼저_잡은_쪽이_실패해_락을_풀면_이어받아_조회한다() {
		String other = lock.tryAcquire(CELL);
		later(100, () -> lock.release(CELL, other));

		List<Restaurant> found = client(delegate, lock, Duration.ofSeconds(2)).findNearby(HERE);

		assertThat(found).containsExactly(PYONGYANG);
		assertThat(calls).hasValue(1);
		assertThat(lock.isHeld(CELL)).isTrue();
	}

	@Test
	void 조회에_실패하면_락을_풀어_다른_곳이_이어받게_한다() {
		RestaurantClient failing = coordinate -> {
			throw new RestaurantLookupException("실패", null);
		};

		assertThatExceptionOfType(RestaurantLookupException.class)
				.isThrownBy(() -> client(failing, lock, Duration.ofSeconds(1)).findNearby(HERE));
		assertThat(lock.isHeld(CELL)).isFalse();
	}

	@Test
	void 락_저장소에_문제가_있으면_기다리지_않고_조회하고_오류를_센다() {
		CacheLoadLock broken = new CacheLoadLock() {
			@Override
			public String tryAcquire(Object key) {
				throw new IllegalStateException("락 연결 실패");
			}

			@Override
			public void release(Object key, String token) {
				throw new IllegalStateException("락 연결 실패");
			}
		};

		long started = System.nanoTime();
		List<Restaurant> found = client(delegate, broken, Duration.ofSeconds(2)).findNearby(HERE);

		assertThat(found).containsExactly(PYONGYANG);
		assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(1));
		assertThat(meterRegistry.get(CachingRestaurantClient.ERRORS_METRIC).tag("operation", "lock").counter().count())
				.isEqualTo(1);
	}

}
