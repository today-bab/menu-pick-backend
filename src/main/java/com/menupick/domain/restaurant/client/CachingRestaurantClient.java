package com.menupick.domain.restaurant.client;

import java.util.List;

import org.springframework.cache.Cache;

import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 다른 {@link RestaurantClient}의 조회 결과를 캐시한다.
 * <p>
 * 좌표를 소수 셋째 자리(약 100m)로 맞춘 칸 단위로 캐시하고, 조회도 그 칸의 좌표로 한다.
 * 그래서 같은 칸 안에서는 다시 뽑아도 외부 API를 부르지 않는다.
 * 대신 거리는 실제 위치와 최대 수십 m 차이가 날 수 있다.
 */
public class CachingRestaurantClient implements RestaurantClient {

	public static final String CACHE_NAME = "nearby-restaurants";

	private static final double CELL_SCALE = 1_000;

	private final RestaurantClient delegate;
	private final Cache cache;

	public CachingRestaurantClient(RestaurantClient delegate, Cache cache) {
		this.delegate = delegate;
		this.cache = cache;
	}

	@Override
	public List<Restaurant> findNearby(Coordinate coordinate) {
		Coordinate cell = toCell(coordinate);
		try {
			return cache.get(cell, () -> delegate.findNearby(cell));
		}
		catch (Cache.ValueRetrievalException ex) {
			if (ex.getCause() instanceof RuntimeException cause) {
				throw cause;
			}
			throw ex;
		}
	}

	private static Coordinate toCell(Coordinate coordinate) {
		return new Coordinate(
				Math.round(coordinate.lat() * CELL_SCALE) / CELL_SCALE,
				Math.round(coordinate.lng() * CELL_SCALE) / CELL_SCALE);
	}

}
