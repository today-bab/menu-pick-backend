package com.menupick.domain.restaurant.model;

/**
 * 위도·경도 좌표.
 */
public record Coordinate(double lat, double lng) {

	public Coordinate {
		if (lat < -90 || lat > 90) {
			throw new IllegalArgumentException("위도는 -90 ~ 90 사이여야 합니다: " + lat);
		}
		if (lng < -180 || lng > 180) {
			throw new IllegalArgumentException("경도는 -180 ~ 180 사이여야 합니다: " + lng);
		}
	}

}
