package com.menupick.domain.restaurant.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class SearchGridTest {

	private static final Coordinate CITY_HALL = new Coordinate(37.5665, 126.978);

	/** 위도 1도 ≈ 111.32km. */
	private static final double KM_LAT = 1 / 111.32;

	@Test
	void 반경을_감싸는_정사각형을_격자로_나눈다() {
		List<SearchArea> areas = SearchGrid.split(CITY_HALL, 1000, 3);

		assertThat(areas).hasSize(9);
		SearchArea southWest = areas.getFirst();
		SearchArea northEast = areas.getLast();
		assertThat(southWest.minLat()).isCloseTo(CITY_HALL.lat() - KM_LAT, within(1e-9));
		assertThat(northEast.maxLat()).isCloseTo(CITY_HALL.lat() + KM_LAT, within(1e-9));
		// 경도 1도는 위도가 높을수록 짧아지므로 같은 1km라도 경도 차이가 더 크다
		double lngRadius = CITY_HALL.lng() - southWest.minLng();
		assertThat(lngRadius).isCloseTo(KM_LAT / Math.cos(Math.toRadians(CITY_HALL.lat())), within(1e-9));
		assertThat(northEast.maxLng() - CITY_HALL.lng()).isCloseTo(lngRadius, within(1e-9));
	}

	@Test
	void 이웃한_구역은_빈틈없이_맞닿는다() {
		List<SearchArea> areas = SearchGrid.split(CITY_HALL, 1000, 3);

		// 같은 줄의 왼쪽·오른쪽, 같은 열의 아래·위
		assertThat(areas.get(0).maxLng()).isEqualTo(areas.get(1).minLng());
		assertThat(areas.get(0).maxLat()).isEqualTo(areas.get(3).minLat());
	}

	@Test
	void 가운데_구역이_중심을_포함한다() {
		SearchArea middle = SearchGrid.split(CITY_HALL, 1000, 3).get(4);

		assertThat(CITY_HALL.lng()).isBetween(middle.minLng(), middle.maxLng());
		assertThat(CITY_HALL.lat()).isBetween(middle.minLat(), middle.maxLat());
	}

	@Test
	void 구역이_하나면_정사각형_전체다() {
		SearchArea only = SearchGrid.split(CITY_HALL, 500, 1).getFirst();

		assertThat(only.maxLat() - only.minLat()).isCloseTo(KM_LAT, within(1e-9));
	}

	@Test
	void 반경이나_구역_수가_0이하면_나눌_수_없다() {
		assertThatIllegalArgumentException().isThrownBy(() -> SearchGrid.split(CITY_HALL, 0, 3));
		assertThatIllegalArgumentException().isThrownBy(() -> SearchGrid.split(CITY_HALL, 1000, 0));
	}

}
