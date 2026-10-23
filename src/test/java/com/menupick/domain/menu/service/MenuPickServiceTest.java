package com.menupick.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.menu.model.MenuPick;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class MenuPickServiceTest {

	private static final Coordinate HERE = new Coordinate(37.5665, 126.978);

	private static Restaurant restaurant(String name, String menu) {
		return new Restaurant(name, menu, 100, HERE, "https://place/" + name);
	}

	@Test
	void 근처_음식점에서_직전_메뉴를_빼고_뽑는다() {
		Restaurant pyongyang = restaurant("평양면옥", "냉면");
		Restaurant katsu = restaurant("카츠정", "돈까스");
		MenuPickService service = new MenuPickService(coordinate -> List.of(pyongyang, katsu), new MenuPicker());

		MenuPick pick = service.pick(HERE, "냉면");

		assertThat(pick).isEqualTo(new MenuPick("돈까스", List.of(katsu)));
	}

	@Test
	void 근처에_음식점이_없으면_뽑을_수_없다() {
		MenuPickService service = new MenuPickService(coordinate -> List.of(), new MenuPicker());

		assertThatExceptionOfType(NoNearbyRestaurantException.class)
				.isThrownBy(() -> service.pick(HERE, null));
	}

}
