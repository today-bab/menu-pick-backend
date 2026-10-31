package com.menupick.domain.menu.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurant;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.menupick.domain.menu.exception.AllMenusExcludedException;
import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuExclusions;
import com.menupick.domain.menu.model.MenuPick;
import com.menupick.domain.menu.service.MenuPickService;
import com.menupick.domain.restaurant.exception.RestaurantLookupException;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

@WebMvcTest(MenuPickController.class)
class MenuPickControllerTest {

	private static final Coordinate HERE = new Coordinate(37.5665, 126.978);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private MenuPickService menuPickService;

	@Test
	void 뽑힌_메뉴와_음식점을_돌려준다() {
		Restaurant pyongyang = restaurant("평양면옥", "냉면", 210, new Coordinate(37.567, 126.979),
				"http://place.map.kakao.com/1");
		given(menuPickService.pick(HERE, "돈까스", MenuExclusions.NONE)).willReturn(new MenuPick("냉면", List.of(pyongyang)));

		assertThat(mvc.get().uri("/api/menus/pick?lat=37.5665&lng=126.978&exclude=돈까스"))
				.hasStatusOk()
				.bodyJson()
				.isLenientlyEqualTo("""
						{
						  "menu": "냉면",
						  "restaurants": [{
						    "name": "평양면옥",
						    "distanceMeters": 210,
						    "lat": 37.567,
						    "lng": 126.979,
						    "placeUrl": "http://place.map.kakao.com/1"
						  }]
						}
						""");
	}

	@Test
	void 직전_메뉴_없이도_뽑는다() {
		given(menuPickService.pick(HERE, null, MenuExclusions.NONE)).willReturn(new MenuPick("냉면", List.of()));

		assertThat(mvc.get().uri("/api/menus/pick?lat=37.5665&lng=126.978")).hasStatusOk();
	}

	@Test
	void 제외할_대분류와_메뉴를_반복_파라미터로_받는다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of("중식", "일식"), List.of("국밥"));
		given(menuPickService.pick(HERE, null, exclusions)).willReturn(new MenuPick("냉면", List.of()));

		assertThat(mvc.get().uri(
				"/api/menus/pick?lat=37.5665&lng=126.978&excludeCuisine=중식&excludeCuisine=일식&excludeMenu=국밥"))
				.hasStatusOk();
	}

	@Test
	void 대분류를_빼도_다시_넣을_메뉴를_반복_파라미터로_받는다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of("한식"), List.of(), List.of("국밥", "냉면"));
		given(menuPickService.pick(HERE, null, exclusions)).willReturn(new MenuPick("국밥", List.of()));

		assertThat(mvc.get().uri(
				"/api/menus/pick?lat=37.5665&lng=126.978&excludeCuisine=한식&includeMenu=국밥&includeMenu=냉면"))
				.hasStatusOk();
	}

	@Test
	void 쉼표가_든_메뉴_이름을_쪼개지_않는다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of(), List.of("육류,고기"));
		given(menuPickService.pick(HERE, null, exclusions)).willReturn(new MenuPick("냉면", List.of()));

		assertThat(mvc.get().uri("/api/menus/pick").queryParam("lat", "37.5665").queryParam("lng", "126.978")
				.queryParam("excludeMenu", "육류,고기"))
				.hasStatusOk();
	}

	@Test
	void 제외하고_남는_메뉴가_없으면_422() {
		given(menuPickService.pick(any(), isNull(), any()))
				.willThrow(new AllMenusExcludedException(MenuExclusions.NONE));

		assertThat(mvc.get().uri("/api/menus/pick?lat=37.5665&lng=126.978&excludeCuisine=한식"))
				.hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void 좌표_범위를_벗어나면_400() {
		assertThat(mvc.get().uri("/api/menus/pick?lat=91&lng=126.978"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void 좌표가_없으면_400() {
		assertThat(mvc.get().uri("/api/menus/pick?lat=37.5665"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void 근처에_음식점이_없으면_404() {
		given(menuPickService.pick(any(), isNull(), any())).willThrow(new NoNearbyRestaurantException(HERE));

		assertThat(mvc.get().uri("/api/menus/pick?lat=37.5665&lng=126.978"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void 음식점_조회에_실패하면_502() {
		given(menuPickService.pick(any(), isNull(), any()))
				.willThrow(new RestaurantLookupException("실패", null));

		assertThat(mvc.get().uri("/api/menus/pick?lat=37.5665&lng=126.978"))
				.hasStatus(HttpStatus.BAD_GATEWAY)
				.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

}
