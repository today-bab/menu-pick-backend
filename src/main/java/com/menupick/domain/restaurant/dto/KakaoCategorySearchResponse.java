package com.menupick.domain.restaurant.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 카카오 로컬 API 카테고리 검색 응답.
 *
 * @see <a href="https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-by-category">카테고리로 장소 검색하기</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoCategorySearchResponse(Meta meta, List<Document> documents) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Meta(@JsonProperty("is_end") boolean isEnd) {
	}

	/**
	 * 장소 하나. 카카오는 좌표와 거리를 문자열로 준다.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Document(
			@JsonProperty("place_name") String placeName,
			@JsonProperty("category_name") String categoryName,
			String distance,
			String x,
			String y,
			@JsonProperty("place_url") String placeUrl) {

		private static final String CATEGORY_SEPARATOR = ">";

		/** 메뉴로 쓰는 가장 세부 단계 ("음식점 > 대분류 > 중분류"). 그 아래는 대개 브랜드다. */
		private static final int MENU_LEVEL = 2;

		/** 중분류 자리에 브랜드가 오는 대분류 (예: 패스트푸드 > 맥도날드, 치킨 > BBQ). 이때는 대분류를 쓴다. */
		private static final Set<String> BRAND_AT_MENU_LEVEL = Set.of("분식", "패스트푸드", "치킨", "샐러드");

		public Restaurant toRestaurant() {
			return new Restaurant(
					placeName,
					menu(),
					Integer.parseInt(distance),
					new Coordinate(Double.parseDouble(y), Double.parseDouble(x)),
					placeUrl);
		}

		/**
		 * 분류에서 브랜드를 뺀 가장 세부 단계를 메뉴로 본다.
		 * 예: "음식점 > 한식 > 국밥 > 더진국" → "국밥", "음식점 > 패스트푸드 > 맥도날드" → "패스트푸드"
		 */
		private String menu() {
			String[] levels = Arrays.stream(categoryName.split(CATEGORY_SEPARATOR))
					.map(String::trim)
					.toArray(String[]::new);
			int level = Math.min(levels.length - 1, MENU_LEVEL);
			if (level == MENU_LEVEL && BRAND_AT_MENU_LEVEL.contains(levels[level - 1])) {
				level--;
			}
			return levels[level];
		}

	}

}
