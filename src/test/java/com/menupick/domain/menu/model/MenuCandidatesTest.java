package com.menupick.domain.menu.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class MenuCandidatesTest {

	private static Restaurant restaurant(String name, String menu, int distanceMeters) {
		return new Restaurant(name, menu, distanceMeters, new Coordinate(37.5, 127), "https://place/" + name);
	}

	@Test
	void 음식점을_메뉴별로_묶는다() {
		Restaurant pyongyang = restaurant("평양면옥", "냉면", 100);
		Restaurant katsu = restaurant("카츠정", "돈까스", 200);
		Restaurant hamheung = restaurant("함흥냉면", "냉면", 300);

		MenuCandidates candidates = MenuCandidates.from(List.of(pyongyang, katsu, hamheung));

		assertThat(candidates.menus()).containsExactly("냉면", "돈까스");
		assertThat(candidates.restaurantsOf("냉면")).containsExactly(pyongyang, hamheung);
		assertThat(candidates.restaurantsOf("돈까스")).containsExactly(katsu);
	}

	@Test
	void 없는_메뉴의_음식점은_빈_목록이다() {
		MenuCandidates candidates = MenuCandidates.from(List.of(restaurant("카츠정", "돈까스", 200)));

		assertThat(candidates.restaurantsOf("냉면")).isEmpty();
	}

	@Test
	void 음식점이_없으면_후보도_비어_있다() {
		assertThat(MenuCandidates.from(List.of()).isEmpty()).isTrue();
	}

}
