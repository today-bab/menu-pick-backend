package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.menupick.domain.restaurant.config.KakaoProperties;
import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class KakaoRestaurantClientTest {

	private static final String BASE_URL = "https://dapi.kakao.com";
	private static final Coordinate SEOUL_CITY_HALL = new Coordinate(37.5665, 126.978);

	private MockRestServiceServer server;
	private RestClient restClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		restClient = builder.build();
	}

	private KakaoRestaurantClient client(int maxPages) {
		KakaoProperties properties = new KakaoProperties("test-key", BASE_URL, Duration.ofSeconds(1),
				Duration.ofSeconds(1), 1000, maxPages, 1);
		return new KakaoRestaurantClient(restClient, properties);
	}

	private static String page(boolean isEnd, String... placeNames) {
		StringBuilder documents = new StringBuilder();
		for (String placeName : placeNames) {
			if (!documents.isEmpty()) {
				documents.append(',');
			}
			documents.append("""
					{"place_name":"%s","category_name":"음식점 > 한식 > 냉면","distance":"120",
					 "x":"126.979","y":"37.567","place_url":"http://place.map.kakao.com/1","id":"1"}
					""".formatted(placeName));
		}
		return """
				{"meta":{"is_end":%s,"total_count":30,"pageable_count":30},"documents":[%s]}
				""".formatted(isEnd, documents);
	}

	private void expectPage(int page, String body) {
		server.expect(once(), requestTo(startsWith(BASE_URL + "/v2/local/search/category.json")))
				.andExpect(queryParam("category_group_code", "FD6"))
				.andExpect(queryParam("x", "126.978"))
				.andExpect(queryParam("y", "37.5665"))
				.andExpect(queryParam("radius", "1000"))
				.andExpect(queryParam("sort", "distance"))
				.andExpect(queryParam("page", String.valueOf(page)))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	@Test
	void 음식점을_조회해_도메인_모델로_바꾼다() {
		expectPage(1, page(true, "평양면옥"));

		List<Restaurant> restaurants = client(3).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).containsExactly(new Restaurant("평양면옥", "냉면", 120,
				new Coordinate(37.567, 126.979), "http://place.map.kakao.com/1"));
		server.verify();
	}

	@Test
	void 마지막_페이지까지_이어서_조회한다() {
		expectPage(1, page(false, "가게1"));
		expectPage(2, page(true, "가게2"));

		List<Restaurant> restaurants = client(3).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).extracting(Restaurant::name).containsExactly("가게1", "가게2");
		server.verify();
	}

	@Test
	void 최대_페이지까지만_조회한다() {
		expectPage(1, page(false, "가게1"));
		expectPage(2, page(false, "가게2"));

		List<Restaurant> restaurants = client(2).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).hasSize(2);
		server.verify();
	}

	@Test
	void 카카오_호출이_실패하면_조회_실패_예외로_바꾼다() {
		server.expect(once(), requestTo(startsWith(BASE_URL)))
				.andRespond(withServerError());

		assertThatExceptionOfType(RestaurantLookupException.class)
				.isThrownBy(() -> client(3).findNearby(SEOUL_CITY_HALL));
	}

}
