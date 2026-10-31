package com.menupick.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurantOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuCategory;
import com.menupick.domain.restaurant.model.Coordinate;

class MenuCategoryServiceTest {

	private static final Coordinate HERE = new Coordinate(37.5665, 126.978);

	@Test
	void 근처_메뉴를_대분류별로_돌려준다() {
		MenuCategoryService service = new MenuCategoryService(coordinate -> List.of(
				restaurantOf("한식", "할매순대국", "국밥"),
				restaurantOf("중식", "홍콩반점", "중식"),
				restaurantOf("한식", "평양면옥", "냉면")));

		assertThat(service.categories(HERE)).containsExactly(
				new MenuCategory("한식", List.of("국밥", "냉면")),
				new MenuCategory("중식", List.of("중식")));
	}

	@Test
	void 근처에_음식점이_없으면_돌려줄_수_없다() {
		MenuCategoryService service = new MenuCategoryService(coordinate -> List.of());

		assertThatExceptionOfType(NoNearbyRestaurantException.class)
				.isThrownBy(() -> service.categories(HERE));
	}

}
