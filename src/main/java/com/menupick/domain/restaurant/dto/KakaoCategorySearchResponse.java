package com.menupick.domain.restaurant.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
	 *
	 * @param id 카카오 장소 id (여러 구역에서 같은 가게가 나올 때 중복 제거에 쓴다)
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Document(
			String id,
			@JsonProperty("place_name") String placeName,
			@JsonProperty("category_name") String categoryName,
			String distance,
			String x,
			String y,
			@JsonProperty("place_url") String placeUrl) {

		private static final String CATEGORY_SEPARATOR = ">";

		/** 대분류 단계 ("음식점 > 대분류"). */
		private static final int CUISINE_LEVEL = 1;

		/** 메뉴로 쓰는 가장 세부 단계 ("음식점 > 대분류 > 중분류"). 그 아래는 대개 브랜드다. */
		private static final int MENU_LEVEL = 2;

		/** 중분류 자리에 브랜드가 오는 대분류 (예: 패스트푸드 > 맥도날드, 치킨 > BBQ). 이때는 대분류를 쓴다. */
		private static final Set<String> BRAND_AT_MENU_LEVEL = Set.of(
				"분식", "패스트푸드", "치킨", "샐러드", "샤브샤브", "패밀리레스토랑", "도시락");

		/**
		 * 같은 메뉴인데 분류 깊이에 따라 이름이 달라지는 것을 하나로 합친다.
		 * 예: 중분류 없이 "음식점 > 중식"인 가게와 "음식점 > 중식 > 중국요리"인 가게는 모두 "중식"
		 */
		private static final Map<String, String> MENU_ALIASES = Map.of(
				"중국요리", "중식",
				"일식집", "일식");

		public Restaurant toRestaurant() {
			String[] levels = levels();
			return new Restaurant(
					placeName,
					cuisine(levels),
					menu(levels),
					Integer.parseInt(distance),
					new Coordinate(Double.parseDouble(y), Double.parseDouble(x)),
					placeUrl);
		}

		private String[] levels() {
			return Arrays.stream(categoryName.split(CATEGORY_SEPARATOR))
					.map(String::trim)
					.toArray(String[]::new);
		}

		/**
		 * "음식점" 바로 아래 단계를 대분류로 본다. 분류가 "음식점" 하나뿐이면 그대로 쓴다.
		 * 예: "음식점 > 한식 > 국밥" → "한식", "음식점 > 패스트푸드 > 맥도날드" → "패스트푸드"
		 */
		private static String cuisine(String[] levels) {
			return levels[Math.min(levels.length - 1, CUISINE_LEVEL)];
		}

		/**
		 * 분류에서 브랜드를 뺀 가장 세부 단계를 메뉴로 본다.
		 * 예: "음식점 > 한식 > 국밥 > 더진국" → "국밥", "음식점 > 패스트푸드 > 맥도날드" → "패스트푸드"
		 */
		private static String menu(String[] levels) {
			int level = Math.min(levels.length - 1, MENU_LEVEL);
			if (level == MENU_LEVEL && BRAND_AT_MENU_LEVEL.contains(levels[level - 1])) {
				level--;
			}
			return MENU_ALIASES.getOrDefault(levels[level], levels[level]);
		}

	}

}
