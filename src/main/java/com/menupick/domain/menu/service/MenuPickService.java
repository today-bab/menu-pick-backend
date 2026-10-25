package com.menupick.domain.menu.service;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuCandidates;
import com.menupick.domain.menu.model.MenuPick;
import com.menupick.domain.restaurant.client.RestaurantClient;
import com.menupick.domain.restaurant.model.Coordinate;

/**
 * 근처 음식점에서 오늘의 메뉴를 뽑는다.
 */
@Service
public class MenuPickService {

	private final RestaurantClient restaurantClient;
	private final MenuPicker menuPicker;
	private final RestaurantSampler restaurantSampler;

	public MenuPickService(RestaurantClient restaurantClient, MenuPicker menuPicker,
			RestaurantSampler restaurantSampler) {
		this.restaurantClient = restaurantClient;
		this.menuPicker = menuPicker;
		this.restaurantSampler = restaurantSampler;
	}

	/**
	 * @param excludedMenu 직전에 뽑은 메뉴. 다른 후보가 있으면 다시 뽑지 않는다.
	 * @throws NoNearbyRestaurantException 근처에 음식점이 없을 때
	 */
	public MenuPick pick(Coordinate coordinate, @Nullable String excludedMenu) {
		MenuCandidates candidates = MenuCandidates.from(restaurantClient.findNearby(coordinate));
		if (candidates.isEmpty()) {
			throw new NoNearbyRestaurantException(coordinate);
		}
		MenuPick pick = menuPicker.pick(candidates, excludedMenu);
		return new MenuPick(pick.menu(), restaurantSampler.sample(pick.restaurants()));
	}

}
