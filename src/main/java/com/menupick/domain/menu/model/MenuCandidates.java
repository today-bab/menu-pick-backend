package com.menupick.domain.menu.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 근처 음식점을 메뉴별로 묶은 뽑기 후보.
 * 메뉴 안의 음식점은 받은 순서(가까운 순)를 유지한다.
 */
public final class MenuCandidates {

	private final Map<String, List<Restaurant>> restaurantsByMenu;

	private MenuCandidates(Map<String, List<Restaurant>> restaurantsByMenu) {
		this.restaurantsByMenu = restaurantsByMenu;
	}

	public static MenuCandidates from(List<Restaurant> restaurants) {
		Map<String, List<Restaurant>> restaurantsByMenu = new LinkedHashMap<>();
		for (Restaurant restaurant : restaurants) {
			restaurantsByMenu.computeIfAbsent(restaurant.menu(), menu -> new ArrayList<>()).add(restaurant);
		}
		restaurantsByMenu.replaceAll((menu, grouped) -> List.copyOf(grouped));
		return new MenuCandidates(restaurantsByMenu);
	}

	public boolean isEmpty() {
		return restaurantsByMenu.isEmpty();
	}

	public List<String> menus() {
		return List.copyOf(restaurantsByMenu.keySet());
	}

	public List<Restaurant> restaurantsOf(String menu) {
		return restaurantsByMenu.getOrDefault(menu, List.of());
	}

}
