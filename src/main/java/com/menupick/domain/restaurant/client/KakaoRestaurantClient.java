package com.menupick.domain.restaurant.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.menupick.domain.restaurant.config.KakaoProperties;
import com.menupick.domain.restaurant.dto.KakaoCategorySearchResponse;
import com.menupick.domain.restaurant.dto.KakaoCategorySearchResponse.Document;
import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 카카오 로컬 API의 카테고리 검색으로 근처 음식점을 조회한다.
 * 결과가 끝나거나 최대 페이지에 닿을 때까지 거리순으로 이어서 조회한다.
 */
public class KakaoRestaurantClient implements RestaurantClient {

	private static final Logger log = LoggerFactory.getLogger(KakaoRestaurantClient.class);

	private static final String CATEGORY_SEARCH_PATH = "/v2/local/search/category.json";
	/** 음식점 카테고리 (카페는 CE7로 따로 분류된다). */
	private static final String RESTAURANT_CATEGORY = "FD6";
	private static final int PAGE_SIZE = 15;

	private final RestClient restClient;
	private final int radiusMeters;
	private final int maxPages;

	/**
	 * @param restClient 카카오 주소와 인증 헤더가 설정된 클라이언트
	 */
	public KakaoRestaurantClient(RestClient restClient, KakaoProperties properties) {
		this.restClient = restClient;
		this.radiusMeters = properties.radiusMeters();
		this.maxPages = properties.maxPages();
	}

	@Override
	public List<Restaurant> findNearby(Coordinate coordinate) {
		List<Restaurant> restaurants = new ArrayList<>();
		for (int page = 1; page <= maxPages; page++) {
			KakaoCategorySearchResponse response = search(coordinate, page);
			response.documents().stream()
					.map(Document::toRestaurant)
					.forEach(restaurants::add);
			if (response.meta().isEnd()) {
				break;
			}
		}
		return restaurants;
	}

	private KakaoCategorySearchResponse search(Coordinate coordinate, int page) {
		log.debug("카카오 음식점 검색: {}, page={}", coordinate, page);
		try {
			KakaoCategorySearchResponse response = restClient.get()
					.uri(uri -> uri.path(CATEGORY_SEARCH_PATH)
							.queryParam("category_group_code", RESTAURANT_CATEGORY)
							.queryParam("x", coordinate.lng())
							.queryParam("y", coordinate.lat())
							.queryParam("radius", radiusMeters)
							.queryParam("sort", "distance")
							.queryParam("page", page)
							.queryParam("size", PAGE_SIZE)
							.build())
					.retrieve()
					.body(KakaoCategorySearchResponse.class);
			if (response == null) {
				throw new RestaurantLookupException("카카오 음식점 검색 응답이 비어 있습니다", null);
			}
			return response;
		}
		catch (RestClientException ex) {
			throw new RestaurantLookupException("카카오 음식점 검색에 실패했습니다", ex);
		}
	}

}
