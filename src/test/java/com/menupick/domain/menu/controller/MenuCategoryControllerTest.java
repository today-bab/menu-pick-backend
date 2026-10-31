package com.menupick.domain.menu.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuCategory;
import com.menupick.domain.menu.service.MenuCategoryService;
import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;

@WebMvcTest(MenuCategoryController.class)
class MenuCategoryControllerTest {

	private static final Coordinate HERE = new Coordinate(37.5665, 126.978);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private MenuCategoryService menuCategoryService;

	@Test
	void 근처_메뉴를_대분류별로_돌려준다() {
		given(menuCategoryService.categories(HERE)).willReturn(List.of(
				new MenuCategory("한식", List.of("국밥", "육류,고기")),
				new MenuCategory("중식", List.of("중식"))));

		assertThat(mvc.get().uri("/api/menus/categories?lat=37.5665&lng=126.978"))
				.hasStatusOk()
				.bodyJson()
				.isStrictlyEqualTo("""
						{
						  "categories": [
						    { "name": "한식", "menus": ["국밥", "육류,고기"] },
						    { "name": "중식", "menus": ["중식"] }
						  ]
						}
						""");
	}

	@Test
	void 좌표_범위를_벗어나면_400() {
		assertThat(mvc.get().uri("/api/menus/categories?lat=37.5665&lng=181"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void 근처에_음식점이_없으면_404() {
		given(menuCategoryService.categories(any())).willThrow(new NoNearbyRestaurantException(HERE));

		assertThat(mvc.get().uri("/api/menus/categories?lat=37.5665&lng=126.978"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void 음식점_조회에_실패하면_502() {
		given(menuCategoryService.categories(any())).willThrow(new RestaurantLookupException("실패", null));

		assertThat(mvc.get().uri("/api/menus/categories?lat=37.5665&lng=126.978"))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

}
