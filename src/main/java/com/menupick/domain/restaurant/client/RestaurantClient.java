package com.menupick.domain.restaurant.client;

import java.util.List;

import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 좌표 근처의 음식점을 조회한다.
 */
public interface RestaurantClient {

	/**
	 * @return 가까운 순으로 정렬된 음식점 목록. 없으면 빈 목록.
	 * @throws RestaurantLookupException 외부 조회에 실패했을 때
	 */
	List<Restaurant> findNearby(Coordinate coordinate);

}
