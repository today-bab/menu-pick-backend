package com.menupick.domain.restaurant.model;

/**
 * 경도·위도로 나타낸 사각형 검색 구역.
 *
 * @param minLng 서쪽 경도
 * @param minLat 남쪽 위도
 * @param maxLng 동쪽 경도
 * @param maxLat 북쪽 위도
 */
public record SearchArea(double minLng, double minLat, double maxLng, double maxLat) {

	public SearchArea {
		if (minLng >= maxLng || minLat >= maxLat) {
			throw new IllegalArgumentException("구역의 서쪽·남쪽 값이 동쪽·북쪽 값보다 작아야 합니다");
		}
	}

}
