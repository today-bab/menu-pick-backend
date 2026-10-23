package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class CachingRestaurantClientTest {

	private static final Restaurant PYONGYANG = new Restaurant("평양면옥", "냉면", 100,
			new Coordinate(37.567, 126.979), "http://place.map.kakao.com/1");

	/** 받은 좌표를 기록하는 가짜 클라이언트. */
	private static class RecordingClient implements RestaurantClient {

		private final List<Coordinate> requested = new ArrayList<>();

		@Override
		public List<Restaurant> findNearby(Coordinate coordinate) {
			requested.add(coordinate);
			return List.of(PYONGYANG);
		}

	}

	private final RecordingClient delegate = new RecordingClient();
	private final CachingRestaurantClient client = new CachingRestaurantClient(delegate,
			new ConcurrentMapCache(CachingRestaurantClient.CACHE_NAME));

	@Test
	void 같은_칸_안의_좌표는_한_번만_조회한다() {
		List<Restaurant> first = client.findNearby(new Coordinate(37.56661, 126.97801));
		List<Restaurant> second = client.findNearby(new Coordinate(37.56689, 126.97779));

		assertThat(first).isEqualTo(second).containsExactly(PYONGYANG);
		assertThat(delegate.requested).containsExactly(new Coordinate(37.567, 126.978));
	}

	@Test
	void 다른_칸의_좌표는_따로_조회한다() {
		client.findNearby(new Coordinate(37.5665, 126.978));
		client.findNearby(new Coordinate(37.5705, 126.978));

		assertThat(delegate.requested).hasSize(2);
	}

	@Test
	void 조회_실패는_캐시하지_않고_그대로_던진다() {
		RestaurantLookupException failure = new RestaurantLookupException("실패", null);
		CachingRestaurantClient failing = new CachingRestaurantClient(coordinate -> {
			throw failure;
		}, new ConcurrentMapCache(CachingRestaurantClient.CACHE_NAME));

		assertThatExceptionOfType(RestaurantLookupException.class)
				.isThrownBy(() -> failing.findNearby(new Coordinate(37.5665, 126.978)))
				.isSameAs(failure);
	}

}
