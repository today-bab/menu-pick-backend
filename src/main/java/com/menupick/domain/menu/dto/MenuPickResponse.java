package com.menupick.domain.menu.dto;

import java.util.List;

import com.menupick.domain.menu.model.MenuPick;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 메뉴 뽑기 응답.
 */
public record MenuPickResponse(String menu, List<RestaurantResponse> restaurants) {

	public record RestaurantResponse(String name, int distanceMeters, double lat, double lng, String placeUrl) {

		static RestaurantResponse from(Restaurant restaurant) {
			return new RestaurantResponse(
					restaurant.name(),
					restaurant.distanceMeters(),
					restaurant.coordinate().lat(),
					restaurant.coordinate().lng(),
					restaurant.placeUrl());
		}

	}

	public static MenuPickResponse from(MenuPick pick) {
		return new MenuPickResponse(
				pick.menu(),
				pick.restaurants().stream().map(RestaurantResponse::from).toList());
	}

}
