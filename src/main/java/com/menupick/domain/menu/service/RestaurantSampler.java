package com.menupick.domain.menu.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 뽑힌 메뉴의 가게가 많으면 일부만 무작위로 골라 보여 준다.
 * <p>
 * 가까운 순으로 자르면 같은 메뉴가 다시 나올 때마다 같은 가게만 보이므로, 무작위로 고른 뒤 가까운 순으로 정렬한다.
 */
@Component
public class RestaurantSampler {

	/** 응답(목록·지도)에 담는 가게 수. */
	static final int MAX_RESTAURANTS = 15;

	private final RandomGenerator random;

	/** {@link MenuPicker}와 같은 이유로 {@link ThreadLocalRandom}을 쓴다. */
	public RestaurantSampler() {
		this(ThreadLocalRandom.current());
	}

	RestaurantSampler(RandomGenerator random) {
		this.random = random;
	}

	/**
	 * @return {@link #MAX_RESTAURANTS}곳 이하면 그대로, 많으면 무작위로 고른 {@link #MAX_RESTAURANTS}곳 (가까운 순)
	 */
	public List<Restaurant> sample(List<Restaurant> restaurants) {
		if (restaurants.size() <= MAX_RESTAURANTS) {
			return restaurants;
		}
		// 앞에서부터 남은 것 중 하나와 바꾸는 부분 셔플 (Fisher–Yates)
		List<Restaurant> shuffled = new ArrayList<>(restaurants);
		for (int i = 0; i < MAX_RESTAURANTS; i++) {
			int j = i + random.nextInt(shuffled.size() - i);
			shuffled.set(i, shuffled.set(j, shuffled.get(i)));
		}
		return shuffled.subList(0, MAX_RESTAURANTS).stream()
				.sorted(Comparator.comparingInt(Restaurant::distanceMeters))
				.toList();
	}

}
