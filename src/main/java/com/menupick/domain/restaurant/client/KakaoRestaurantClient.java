package com.menupick.domain.restaurant.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import com.menupick.domain.restaurant.model.SearchArea;
import com.menupick.domain.restaurant.model.SearchGrid;

/**
 * 카카오 로컬 API의 카테고리 검색으로 근처 음식점을 조회한다.
 * <p>
 * 카카오는 한 번 검색에 최대 45곳(15곳 × 3페이지)만 주므로, 반경을 {@link SearchGrid} 구역으로 나눠
 * 구역마다 동시에 검색한 뒤 합친다. 구역 안에서는 결과가 끝나거나 최대 페이지에 닿을 때까지 이어서 조회한다.
 * 거리는 모두 요청 좌표 기준이며, 구역이 반경을 감싸는 정사각형이라 반경 밖(모서리) 가게는 뺀다.
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
	private final int gridSize;

	/**
	 * @param restClient 카카오 주소와 인증 헤더가 설정된 클라이언트
	 */
	public KakaoRestaurantClient(RestClient restClient, KakaoProperties properties) {
		this.restClient = restClient;
		this.radiusMeters = properties.radiusMeters();
		this.maxPages = properties.maxPages();
		this.gridSize = properties.gridSize();
	}

	@Override
	public List<Restaurant> findNearby(Coordinate coordinate) {
		List<SearchArea> areas = SearchGrid.split(coordinate, radiusMeters, gridSize);
		// 구역 경계에 걸친 가게는 두 구역에서 모두 나올 수 있어 카카오 장소 id로 한 번만 남긴다
		Map<String, Document> documents = new LinkedHashMap<>();
		for (List<Document> found : searchAll(coordinate, areas)) {
			found.forEach(document -> documents.putIfAbsent(document.id(), document));
		}
		return documents.values().stream()
				.map(Document::toRestaurant)
				.filter(restaurant -> restaurant.distanceMeters() <= radiusMeters)
				.sorted(Comparator.comparingInt(Restaurant::distanceMeters))
				.toList();
	}

	/** 구역들을 가상 스레드로 동시에 검색한다. 한 구역이라도 실패하면 전체를 실패로 본다. */
	private List<List<Document>> searchAll(Coordinate coordinate, List<SearchArea> areas) {
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<List<Document>>> futures = areas.stream()
					.map(area -> executor.submit(() -> searchArea(coordinate, area)))
					.toList();
			List<List<Document>> results = new ArrayList<>(futures.size());
			for (Future<List<Document>> future : futures) {
				results.add(await(future));
			}
			return results;
		}
	}

	private static List<Document> await(Future<List<Document>> future) {
		try {
			return future.get();
		}
		catch (ExecutionException ex) {
			if (ex.getCause() instanceof RuntimeException cause) {
				throw cause;
			}
			throw new RestaurantLookupException("카카오 음식점 검색에 실패했습니다", ex.getCause());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RestaurantLookupException("카카오 음식점 검색이 중단됐습니다", ex);
		}
	}

	private List<Document> searchArea(Coordinate coordinate, SearchArea area) {
		List<Document> documents = new ArrayList<>();
		for (int page = 1; page <= maxPages; page++) {
			KakaoCategorySearchResponse response = search(coordinate, area, page);
			documents.addAll(response.documents());
			if (response.meta().isEnd()) {
				break;
			}
		}
		return documents;
	}

	private KakaoCategorySearchResponse search(Coordinate coordinate, SearchArea area, int page) {
		log.debug("카카오 음식점 검색: {}, {}, page={}", coordinate, area, page);
		try {
			KakaoCategorySearchResponse response = restClient.get()
					.uri(uri -> uri.path(CATEGORY_SEARCH_PATH)
							.queryParam("category_group_code", RESTAURANT_CATEGORY)
							// x·y는 거리 계산과 정렬 기준, rect는 검색 범위
							.queryParam("x", coordinate.lng())
							.queryParam("y", coordinate.lat())
							.queryParam("rect", toRect(area))
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

	/** 카카오 rect 형식: 왼쪽 X, 왼쪽 Y, 오른쪽 X, 오른쪽 Y (경도·위도). */
	static String toRect(SearchArea area) {
		return area.minLng() + "," + area.minLat() + "," + area.maxLng() + "," + area.maxLat();
	}

}
