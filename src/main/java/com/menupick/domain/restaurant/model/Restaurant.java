package com.menupick.domain.restaurant.model;

/**
 * 근처 음식점.
 *
 * @param name 가게 이름
 * @param cuisine 메뉴가 속한 대분류 (예: 한식). 사용자가 대분류 통째로 추천에서 뺄 때 쓴다.
 * @param menu 가게가 파는 대표 메뉴 (예: 냉면)
 * @param distanceMeters 기준 좌표로부터의 거리
 * @param coordinate 가게 위치
 * @param placeUrl 가게 상세 페이지 주소
 */
public record Restaurant(
		String name,
		String cuisine,
		String menu,
		int distanceMeters,
		Coordinate coordinate,
		String placeUrl) {
}
