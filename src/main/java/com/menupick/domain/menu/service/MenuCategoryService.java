package com.menupick.domain.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuCandidates;
import com.menupick.domain.menu.model.MenuCategory;
import com.menupick.domain.restaurant.client.RestaurantClient;
import com.menupick.domain.restaurant.model.Coordinate;

/**
 * 근처에서 뽑을 수 있는 메뉴를 대분류별로 보여 준다. 사용자가 제외할 대분류·메뉴를 고를 때 쓴다.
 * <p>
 * 뽑기와 같은 음식점 조회(캐시)를 쓰므로, 여기 나온 메뉴 이름을 그대로 제외 조건으로 보내면 뽑기에서 빠진다.
 */
@Service
public class MenuCategoryService {

	private final RestaurantClient restaurantClient;

	public MenuCategoryService(RestaurantClient restaurantClient) {
		this.restaurantClient = restaurantClient;
	}

	/**
	 * @throws NoNearbyRestaurantException 근처에 음식점이 없을 때
	 */
	public List<MenuCategory> categories(Coordinate coordinate) {
		MenuCandidates candidates = MenuCandidates.from(restaurantClient.findNearby(coordinate));
		if (candidates.isEmpty()) {
			throw new NoNearbyRestaurantException(coordinate);
		}
		return candidates.categories();
	}

}
