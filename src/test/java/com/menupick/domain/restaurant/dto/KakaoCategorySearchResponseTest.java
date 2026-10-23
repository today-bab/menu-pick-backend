package com.menupick.domain.restaurant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.menupick.domain.restaurant.dto.KakaoCategorySearchResponse.Document;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class KakaoCategorySearchResponseTest {

	private static Document document(String categoryName) {
		return new Document("평양면옥", categoryName, "210", "127.001", "37.502",
				"http://place.map.kakao.com/123");
	}

	@Test
	void 장소를_음식점으로_바꾼다() {
		Restaurant restaurant = document("음식점 > 한식 > 냉면").toRestaurant();

		assertThat(restaurant).isEqualTo(new Restaurant(
				"평양면옥", "냉면", 210, new Coordinate(37.502, 127.001), "http://place.map.kakao.com/123"));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"음식점 > 한식 > 육류,고기 > 삼겹살 | 삼겹살",
			"음식점 > 일식                | 일식",
			"음식점                       | 음식점",
	})
	void 분류의_가장_세부_단계를_메뉴로_본다(String categoryName, String menu) {
		assertThat(document(categoryName).toRestaurant().menu()).isEqualTo(menu);
	}

}
