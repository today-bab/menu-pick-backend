package com.menupick.domain.restaurant.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 반경 검색을 여러 사각형 구역으로 나눈다.
 * <p>
 * 카카오 로컬 API는 한 번 검색에 최대 45곳만 주므로, 가게가 많은 곳에서 반경으로 한 번만 찾으면
 * 가장 가까운 수십 m 안의 가게만 받는다. 반경을 감싸는 정사각형을 {@code gridSize × gridSize} 구역으로 나눠
 * 구역마다 따로 찾으면 반경 전체에서 고르게 받을 수 있다.
 */
public final class SearchGrid {

	/** 위도 1도의 거리 (m). 경도 1도는 여기에 cos(위도)를 곱한다. */
	private static final double METERS_PER_DEGREE = 111_320;

	private SearchGrid() {
	}

	/**
	 * @return 서쪽→동쪽, 남쪽→북쪽 순서의 구역 {@code gridSize²}개. 모두 합치면 반경을 감싸는 정사각형이 된다.
	 */
	public static List<SearchArea> split(Coordinate center, int radiusMeters, int gridSize) {
		if (radiusMeters <= 0 || gridSize <= 0) {
			throw new IllegalArgumentException("반경과 구역 수는 0보다 커야 합니다");
		}
		double latRadius = radiusMeters / METERS_PER_DEGREE;
		double lngRadius = radiusMeters / (METERS_PER_DEGREE * Math.cos(Math.toRadians(center.lat())));
		double minLat = center.lat() - latRadius;
		double minLng = center.lng() - lngRadius;
		double latStep = 2 * latRadius / gridSize;
		double lngStep = 2 * lngRadius / gridSize;

		List<SearchArea> areas = new ArrayList<>(gridSize * gridSize);
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				areas.add(new SearchArea(
						minLng + col * lngStep, minLat + row * latStep,
						minLng + (col + 1) * lngStep, minLat + (row + 1) * latStep));
			}
		}
		return areas;
	}

}
