package com.menupick.domain.restaurant.dto;

import java.util.List;

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

		public Restaurant toRestaurant() {
			return new Restaurant(
					placeName,
					menu(),
					Integer.parseInt(distance),
					new Coordinate(Double.parseDouble(y), Double.parseDouble(x)),
					placeUrl);
		}

		/**
		 * 분류의 가장 세부 단계를 메뉴로 본다. 예: "음식점 > 한식 > 냉면" → "냉면"
		 */
		private String menu() {
			String[] levels = categoryName.split(CATEGORY_SEPARATOR);
			return levels[levels.length - 1].trim();
		}

	}

}
