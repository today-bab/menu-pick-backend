package com.menupick.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurant;

import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.menupick.domain.restaurant.model.Restaurant;

class RestaurantSamplerTest {

	/** 거리 10m, 20m, … 인 냉면집 {@code count}곳 (가까운 순). */
	private static List<Restaurant> restaurants(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> restaurant("냉면집" + i, "냉면", i * 10))
				.toList();
	}

	/** 남은 것 중 항상 마지막을 고르는 난수 생성기. */
	private static final RandomGenerator ALWAYS_LAST = new RandomGenerator() {
		@Override
		public long nextLong() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextInt(int bound) {
			return bound - 1;
		}
	};

	@Test
	void 가게가_적으면_그대로_돌려준다() {
		List<Restaurant> few = restaurants(RestaurantSampler.MAX_RESTAURANTS);

		assertThat(new RestaurantSampler(ALWAYS_LAST).sample(few)).isEqualTo(few);
	}

	@Test
	void 가게가_많으면_무작위로_골라_가까운_순으로_돌려준다() {
		List<Restaurant> many = restaurants(40);

		List<Restaurant> sampled = new RestaurantSampler(ALWAYS_LAST).sample(many);

		// 가까운 순으로 잘랐다면 들어갈 수 없는 가장 먼 가게도 뽑힌다
		assertThat(sampled).hasSize(RestaurantSampler.MAX_RESTAURANTS)
				.isSortedAccordingTo(Comparator.comparingInt(Restaurant::distanceMeters))
				.contains(many.getLast());
	}

	@Test
	void 같은_가게를_두_번_고르지_않는다() {
		List<Restaurant> sampled = new RestaurantSampler().sample(restaurants(40));

		assertThat(sampled).hasSize(RestaurantSampler.MAX_RESTAURANTS).doesNotHaveDuplicates();
	}

}
