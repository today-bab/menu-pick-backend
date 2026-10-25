package com.menupick.domain.restaurant.client;

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
 * 캐시 저장소(Valkey)에 문제가 생겨도 조회는 막지 않는다. 캐시 오류는 로그와 지표
 * ({@value #ERRORS_METRIC}, 태그 {@code operation=lookup|store})만 남기고 원래 클라이언트로 조회한다.
 */
public class CachingRestaurantClient implements RestaurantClient {

	public static final String CACHE_NAME = "nearby-restaurants";

	public static final String ERRORS_METRIC = "restaurant.cache.errors";

	private static final Logger log = LoggerFactory.getLogger(CachingRestaurantClient.class);

	private static final double CELL_SCALE = 1_000;

	private final RestaurantClient delegate;
	private final Cache cache;
	private final Counter lookupErrors;
	private final Counter storeErrors;

	public CachingRestaurantClient(RestaurantClient delegate, Cache cache, MeterRegistry meterRegistry) {
		this.delegate = delegate;
		this.cache = cache;
		// 오류가 없어도 0으로 지표가 보이도록 미리 등록한다
		this.lookupErrors = errorCounter(meterRegistry, "lookup");
		this.storeErrors = errorCounter(meterRegistry, "store");
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
		List<Restaurant> found = delegate.findNearby(cell);
		store(cell, found);
		return found;
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
