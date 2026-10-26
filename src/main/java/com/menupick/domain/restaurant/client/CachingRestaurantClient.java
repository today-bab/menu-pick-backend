package com.menupick.domain.restaurant.client;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 다른 {@link RestaurantClient}의 조회 결과를 캐시한다.
 * <p>
 * 좌표를 소수 셋째 자리(약 100m)로 맞춘 칸 단위로 캐시하고, 조회도 그 칸의 좌표로 한다.
 * 그래서 같은 칸 안에서는 다시 뽑아도 외부 API를 부르지 않는다.
 * 대신 거리는 실제 위치와 최대 수십 m 차이가 날 수 있다.
 * <p>
 * 캐시가 빈 칸에 요청이 동시에 몰리면 {@link CacheLoadLock}을 잡은 한 곳만 원본을 조회하고,
 * 나머지는 캐시에 값이 생길 때까지 기다린다. 너무 오래 걸리면 기다리지 않고 직접 조회한다.
 * <p>
 * 캐시 저장소(Valkey)에 문제가 생겨도 조회는 막지 않는다. 캐시 오류는 로그와 지표
 * ({@value #ERRORS_METRIC}, 태그 {@code operation=lookup|store|lock})만 남기고 원래 클라이언트로 조회한다.
 */
public class CachingRestaurantClient implements RestaurantClient {

	public static final String CACHE_NAME = "nearby-restaurants";

	public static final String ERRORS_METRIC = "restaurant.cache.errors";

	/**
	 * 캐시 미스를 어떻게 처리했는지 센다. 태그 {@code outcome}:
	 * {@code loaded}(락을 잡고 원본 조회), {@code waited}(다른 곳이 채운 값을 기다려 씀),
	 * {@code timeout}(기다리다 포기하고 원본 조회)
	 */
	public static final String MISSES_METRIC = "restaurant.cache.misses";

	private static final Logger log = LoggerFactory.getLogger(CachingRestaurantClient.class);

	private static final double CELL_SCALE = 1_000;

	/** 락 저장소 오류로 락 없이 조회할 때 쓰는 토큰. 실제 토큰(UUID)과 겹치지 않는다. */
	private static final String NO_LOCK = "";

	private final RestaurantClient delegate;
	private final Cache cache;
	private final CacheLoadLock lock;
	private final Duration waitTimeout;
	private final Duration pollInterval;
	private final Counter lookupErrors;
	private final Counter storeErrors;
	private final Counter lockErrors;
	private final Counter loadedMisses;
	private final Counter waitedMisses;
	private final Counter timedOutMisses;

	/** 락 없이, 캐시 미스마다 바로 원본을 조회한다. */
	public CachingRestaurantClient(RestaurantClient delegate, Cache cache, MeterRegistry meterRegistry) {
		this(delegate, cache, CacheLoadLock.NONE, Duration.ZERO, Duration.ZERO, meterRegistry);
	}

	/**
	 * @param waitTimeout 다른 곳이 채우기를 기다리는 최대 시간. 넘으면 직접 조회한다
	 * @param pollInterval 기다리는 동안 캐시를 다시 확인하는 간격
	 */
	public CachingRestaurantClient(RestaurantClient delegate, Cache cache, CacheLoadLock lock, Duration waitTimeout,
			Duration pollInterval, MeterRegistry meterRegistry) {
		this.delegate = delegate;
		this.cache = cache;
		this.lock = lock;
		this.waitTimeout = waitTimeout;
		this.pollInterval = pollInterval;
		// 오류가 없어도 0으로 지표가 보이도록 미리 등록한다
		this.lookupErrors = errorCounter(meterRegistry, "lookup");
		this.storeErrors = errorCounter(meterRegistry, "store");
		this.lockErrors = errorCounter(meterRegistry, "lock");
		this.loadedMisses = missCounter(meterRegistry, "loaded");
		this.waitedMisses = missCounter(meterRegistry, "waited");
		this.timedOutMisses = missCounter(meterRegistry, "timeout");
	}

	private static Counter missCounter(MeterRegistry meterRegistry, String outcome) {
		return Counter.builder(MISSES_METRIC)
				.description("캐시 미스를 처리한 방법별 횟수")
				.tag("cache", CACHE_NAME)
				.tag("outcome", outcome)
				.register(meterRegistry);
	}

	private static Counter errorCounter(MeterRegistry meterRegistry, String operation) {
		return Counter.builder(ERRORS_METRIC)
				.description("캐시 저장소 오류로 캐시 없이 처리한 횟수")
				.tag("cache", CACHE_NAME)
				.tag("operation", operation)
				.register(meterRegistry);
	}

	@Override
	public List<Restaurant> findNearby(Coordinate coordinate) {
		Coordinate cell = toCell(coordinate);
		List<Restaurant> cached = lookup(cell);
		if (cached != null) {
			return cached;
		}

		String token = acquire(cell);
		long deadline = System.nanoTime() + waitTimeout.toNanos();
		while (token == null && System.nanoTime() < deadline) {
			if (!pause()) {
				break;
			}
			List<Restaurant> loaded = lookup(cell);
			if (loaded != null) {
				waitedMisses.increment();
				return loaded;
			}
			// 먼저 잡은 쪽이 조회에 실패해 락을 풀었으면 이어받아 조회한다
			token = acquire(cell);
		}
		// 기다려도 채워지지 않으면 락 없이 직접 조회한다
		(token == null ? timedOutMisses : loadedMisses).increment();
		return load(cell, token);
	}

	private List<Restaurant> load(Coordinate cell, @Nullable String token) {
		List<Restaurant> found;
		try {
			found = delegate.findNearby(cell);
		}
		catch (RuntimeException ex) {
			release(cell, token);
			throw ex;
		}
		store(cell, found);
		// 성공하면 락을 풀지 않고 만료로 풀리게 둔다. 저장이 비동기로 끝나기 전에 풀면,
		// 기다리던 쪽이 아직 빈 캐시를 보고 락을 잡아 같은 칸을 다시 조회할 수 있다.
		return found;
	}

	/** @return 토큰, 다른 곳이 잡고 있으면 {@code null}, 락 저장소 오류면 {@link #NO_LOCK} */
	private @Nullable String acquire(Coordinate cell) {
		try {
			return lock.tryAcquire(cell);
		}
		catch (RuntimeException ex) {
			lockErrors.increment();
			log.warn("캐시 락 실패, 락 없이 조회한다: {}", ex.toString());
			return NO_LOCK;
		}
	}

	private void release(Coordinate cell, @Nullable String token) {
		if (token == null || NO_LOCK.equals(token)) {
			return;
		}
		try {
			lock.release(cell, token);
		}
		catch (RuntimeException ex) {
			// 풀지 못한 락은 만료 시간이 지나면 풀린다
			lockErrors.increment();
			log.warn("캐시 락 해제 실패: {}", ex.toString());
		}
	}

	/** @return 기다리는 중에 스레드가 중단되면 {@code false} */
	private boolean pause() {
		try {
			Thread.sleep(pollInterval);
			return true;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private @Nullable List<Restaurant> lookup(Coordinate cell) {
		try {
			Cache.ValueWrapper hit = cache.get(cell);
			return (hit != null) ? (List<Restaurant>) hit.get() : null;
		}
		catch (RuntimeException ex) {
			lookupErrors.increment();
			log.warn("캐시 조회 실패, 캐시 없이 조회한다: {}", ex.toString());
			return null;
		}
	}

	private void store(Coordinate cell, List<Restaurant> restaurants) {
		try {
			cache.put(cell, restaurants);
		}
		catch (RuntimeException ex) {
			// 저장이 비동기로 처리되면 실패가 여기까지 오지 않으므로, 오류 감지는 주로 조회 쪽 지표에 기댄다
			storeErrors.increment();
			log.warn("캐시 저장 실패, 결과만 돌려준다: {}", ex.toString());
		}
	}

	private static Coordinate toCell(Coordinate coordinate) {
		return new Coordinate(
				Math.round(coordinate.lat() * CELL_SCALE) / CELL_SCALE,
				Math.round(coordinate.lng() * CELL_SCALE) / CELL_SCALE);
	}

}
