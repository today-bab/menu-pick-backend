package com.menupick.domain.menu.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 근처 음식점을 메뉴별로 묶은 뽑기 후보. 메뉴마다 음식점이 하나 이상 있다.
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

	/**
	 * 제외한 대분류·메뉴를 뺀 후보. 메뉴의 대분류는 {@link #categories()}와 같게 가장 가까운 가게의 것으로 본다.
	 */
	public MenuCandidates excluding(MenuExclusions exclusions) {
		Map<String, List<Restaurant>> remaining = new LinkedHashMap<>(restaurantsByMenu);
		remaining.entrySet().removeIf(entry -> exclusions.excludes(cuisineOf(entry.getValue()), entry.getKey()));
		return new MenuCandidates(remaining);
	}

	private static String cuisineOf(List<Restaurant> restaurantsOfMenu) {
		return restaurantsOfMenu.getFirst().cuisine();
	}

	/**
	 * 메뉴를 대분류별로 묶는다. 대분류와 그 안의 메뉴는 가장 가까운 가게가 먼저 나온 순서다.
	 * <p>
	 * 같은 메뉴가 여러 대분류에 걸쳐 있으면 가장 가까운 가게의 대분류에 넣는다.
	 */
	public List<MenuCategory> categories() {
		Map<String, List<String>> menusByCuisine = new LinkedHashMap<>();
		restaurantsByMenu.forEach((menu, restaurants) -> menusByCuisine
				.computeIfAbsent(cuisineOf(restaurants), cuisine -> new ArrayList<>())
				.add(menu));
		return menusByCuisine.entrySet().stream()
				.map(entry -> new MenuCategory(entry.getKey(), entry.getValue()))
				.toList();
	}

}
