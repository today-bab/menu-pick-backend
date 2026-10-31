package com.menupick.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurant;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurantOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.menupick.domain.menu.exception.AllMenusExcludedException;
import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuExclusions;
import com.menupick.domain.menu.model.MenuPick;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class MenuPickServiceTest {

	private static final Coordinate HERE = new Coordinate(37.5665, 126.978);

	@Test
	void 근처_음식점에서_직전_메뉴를_빼고_뽑는다() {
		Restaurant pyongyang = restaurant("평양면옥", "냉면");
		Restaurant katsu = restaurant("카츠정", "돈까스");
		MenuPickService service = new MenuPickService(coordinate -> List.of(pyongyang, katsu), new MenuPicker(),
				new RestaurantSampler());

		MenuPick pick = service.pick(HERE, "냉면");

		assertThat(pick).isEqualTo(new MenuPick("돈까스", List.of(katsu)));
	}

	@Test
	void 제외한_대분류와_메뉴는_직전_메뉴밖에_안_남아도_뽑지_않는다() {
		Restaurant pyongyang = restaurantOf("한식", "평양면옥", "냉면");
		MenuPickService service = new MenuPickService(
				coordinate -> List.of(restaurantOf("중식", "홍콩반점", "중식"), pyongyang,
						restaurantOf("한식", "할매순대국", "국밥")),
				new MenuPicker(), new RestaurantSampler());

		MenuPick pick = service.pick(HERE, "냉면", MenuExclusions.of(List.of("중식"), List.of("국밥")));

		// 직전 메뉴는 다른 후보가 없으면 다시 뽑지만, 사용자가 뺀 것은 끝까지 뺀다.
		assertThat(pick).isEqualTo(new MenuPick("냉면", List.of(pyongyang)));
	}

	@Test
	void 모두_제외하면_뽑을_수_없다() {
		MenuPickService service = new MenuPickService(
				coordinate -> List.of(restaurantOf("중식", "홍콩반점", "중식")), new MenuPicker(),
				new RestaurantSampler());

		assertThatExceptionOfType(AllMenusExcludedException.class)
				.isThrownBy(() -> service.pick(HERE, null, MenuExclusions.of(List.of("중식"), List.of())));
	}

	@Test
	void 근처에_음식점이_없으면_뽑을_수_없다() {
		MenuPickService service = new MenuPickService(coordinate -> List.of(), new MenuPicker(),
				new RestaurantSampler());

		assertThatExceptionOfType(NoNearbyRestaurantException.class)
				.isThrownBy(() -> service.pick(HERE, null));
	}

}
