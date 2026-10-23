package com.menupick.domain.menu.exception;

import com.menupick.domain.restaurant.model.Coordinate;

/**
 * 근처에 음식점이 하나도 없어 메뉴를 뽑을 수 없을 때 던진다.
 */
public class NoNearbyRestaurantException extends RuntimeException {

	public NoNearbyRestaurantException(Coordinate coordinate) {
		super("근처에 음식점이 없습니다: " + coordinate);
	}

}
