package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
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
import com.menupick.domain.restaurant.model.SearchArea;
import com.menupick.domain.restaurant.model.SearchGrid;

class KakaoRestaurantClientTest {

	private static final String BASE_URL = "https://dapi.kakao.com";
	private static final Coordinate SEOUL_CITY_HALL = new Coordinate(37.5665, 126.978);
	private static final int RADIUS = 1000;

	private MockRestServiceServer server;
	private RestClient restClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		// 구역들을 동시에 검색하므로 요청 순서는 정해지지 않는다
		server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
		restClient = builder.build();
	}

	private KakaoRestaurantClient client(int maxPages, int gridSize) {
		KakaoProperties properties = new KakaoProperties("test-key", BASE_URL, Duration.ofSeconds(1),
				Duration.ofSeconds(1), RADIUS, maxPages, gridSize);
		return new KakaoRestaurantClient(restClient, properties);
	}

	private static String area(int index, int gridSize) {
		SearchArea area = SearchGrid.split(SEOUL_CITY_HALL, RADIUS, gridSize).get(index);
		return KakaoRestaurantClient.toRect(area);
	}

	/** 가게 하나. 이름이 같으면 같은 카카오 장소 id를 쓴다. */
	private static String place(String name, int distance) {
		return """
				{"id":"%s","place_name":"%s","category_name":"음식점 > 한식 > 냉면","distance":"%d",
				 "x":"126.979","y":"37.567","place_url":"http://place.map.kakao.com/%s"}
				""".formatted(name, name, distance, name);
	}

	private static String page(boolean isEnd, String... places) {
		return """
				{"meta":{"is_end":%s,"total_count":30,"pageable_count":30},"documents":[%s]}
				""".formatted(isEnd, String.join(",", places));
	}

	private void expectPage(String rect, int page, String body) {
		server.expect(once(), requestTo(startsWith(BASE_URL + "/v2/local/search/category.json")))
				.andExpect(queryParam("category_group_code", "FD6"))
				.andExpect(queryParam("x", "126.978"))
				.andExpect(queryParam("y", "37.5665"))
				.andExpect(queryParam("rect", rect))
				.andExpect(queryParam("sort", "distance"))
				.andExpect(queryParam("page", String.valueOf(page)))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	@Test
	void 음식점을_조회해_도메인_모델로_바꾼다() {
		expectPage(area(0, 1), 1, page(true, place("평양면옥", 120)));

		List<Restaurant> restaurants = client(3, 1).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).containsExactly(new Restaurant("평양면옥", "냉면", 120,
				new Coordinate(37.567, 126.979), "http://place.map.kakao.com/평양면옥"));
		server.verify();
	}

	@Test
	void 마지막_페이지까지_이어서_조회한다() {
		expectPage(area(0, 1), 1, page(false, place("가게1", 100)));
		expectPage(area(0, 1), 2, page(true, place("가게2", 200)));

		List<Restaurant> restaurants = client(3, 1).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).extracting(Restaurant::name).containsExactly("가게1", "가게2");
		server.verify();
	}

	@Test
	void 최대_페이지까지만_조회한다() {
		expectPage(area(0, 1), 1, page(false, place("가게1", 100)));
		expectPage(area(0, 1), 2, page(false, place("가게2", 200)));

		List<Restaurant> restaurants = client(2, 1).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).hasSize(2);
		server.verify();
	}

	@Test
	void 구역마다_검색해_거리순으로_합친다() {
		expectPage(area(0, 2), 1, page(true, place("남서", 700)));
		expectPage(area(1, 2), 1, page(true, place("남동", 300)));
		expectPage(area(2, 2), 1, page(true, place("북서", 500)));
		expectPage(area(3, 2), 1, page(true, place("북동", 100)));

		List<Restaurant> restaurants = client(3, 2).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).extracting(Restaurant::name).containsExactly("북동", "남동", "북서", "남서");
		server.verify();
	}

	@Test
	void 구역_경계에서_두_번_나온_가게는_한_번만_남긴다() {
		expectPage(area(0, 2), 1, page(true, place("경계", 200), place("남서", 400)));
		expectPage(area(1, 2), 1, page(true, place("경계", 200)));
		expectPage(area(2, 2), 1, page(true));
		expectPage(area(3, 2), 1, page(true));

		List<Restaurant> restaurants = client(3, 2).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).extracting(Restaurant::name).containsExactly("경계", "남서");
	}

	@Test
	void 반경_밖의_모서리_가게는_뺀다() {
		expectPage(area(0, 1), 1, page(true, place("안", 990), place("모서리", 1300)));

		List<Restaurant> restaurants = client(3, 1).findNearby(SEOUL_CITY_HALL);

		assertThat(restaurants).extracting(Restaurant::name).containsExactly("안");
	}

	@Test
	void 한_구역이라도_실패하면_조회_실패_예외로_바꾼다() {
		server.expect(once(), queryParam("rect", area(0, 2))).andRespond(withServerError());
		server.expect(manyTimes(), requestTo(startsWith(BASE_URL)))
				.andRespond(withSuccess(page(true), MediaType.APPLICATION_JSON));

		assertThatExceptionOfType(RestaurantLookupException.class)
				.isThrownBy(() -> client(3, 2).findNearby(SEOUL_CITY_HALL));
	}

}
