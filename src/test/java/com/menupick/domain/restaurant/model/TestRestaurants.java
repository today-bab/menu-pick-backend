package com.menupick.domain.restaurant.model;

/**
 * 테스트에서 쓰는 음식점. 이름·메뉴·거리 말고는 테스트에 상관없는 값을 채운다.
 * <p>
 * {@link Restaurant}에 필드가 늘어도 이 클래스만 고치면 되도록 테스트는 여기서 음식점을 만든다.
 */
public final class TestRestaurants {

	private static final Coordinate ANYWHERE = new Coordinate(37.5, 127);

	private TestRestaurants() {
	}

	/** 좌표와 주소까지 확인하는 테스트(응답 변환 등)에서 쓴다. */
	public static Restaurant restaurant(String name, String menu, int distanceMeters, Coordinate coordinate,
			String placeUrl) {
		return new Restaurant(name, menu, distanceMeters, coordinate, placeUrl);
	}

	public static Restaurant restaurant(String name, String menu, int distanceMeters) {
		return restaurant(name, menu, distanceMeters, ANYWHERE, "https://place/" + name);
	}

	public static Restaurant restaurant(String name, String menu) {
		return restaurant(name, menu, 100);
	}

}
