package com.menupick.domain.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.menupick.domain.menu.model.MenuCandidates;
import com.menupick.domain.menu.model.MenuPick;
import com.menupick.domain.restaurant.model.Coordinate;
import com.menupick.domain.restaurant.model.Restaurant;

class MenuPickerTest {

	private static Restaurant restaurant(String name, String menu) {
		return new Restaurant(name, menu, 100, new Coordinate(37.5, 127), "https://place/" + name);
	}

	private static final Restaurant PYONGYANG = restaurant("평양면옥", "냉면");
	private static final Restaurant KATSU = restaurant("카츠정", "돈까스");
	private static final Restaurant GUKBAP = restaurant("할매순대국", "국밥");

	private static final MenuCandidates CANDIDATES = MenuCandidates.from(List.of(PYONGYANG, KATSU, GUKBAP));

	/** 항상 정해진 위치를 고르는 난수 생성기. */
	private static RandomGenerator alwaysPicking(int index) {
		return new RandomGenerator() {
			@Override
			public long nextLong() {
				throw new UnsupportedOperationException();
			}

			@Override
			public int nextInt(int bound) {
				return index;
			}
		};
	}

	@Test
	void 난수가_가리키는_메뉴와_그_메뉴의_음식점을_돌려준다() {
		MenuPick pick = new MenuPicker(alwaysPicking(1)).pick(CANDIDATES, null);

		assertThat(pick).isEqualTo(new MenuPick("돈까스", List.of(KATSU)));
	}

	@Test
	void 직전_메뉴는_후보에서_뺀다() {
		MenuPicker picker = new MenuPicker(alwaysPicking(0));

		MenuPick pick = picker.pick(CANDIDATES, "냉면");

		assertThat(pick.menu()).isEqualTo("돈까스");
	}

	@Test
	void 직전_메뉴밖에_없으면_그_메뉴를_다시_고른다() {
		MenuCandidates onlyNaengmyeon = MenuCandidates.from(List.of(PYONGYANG));

		MenuPick pick = new MenuPicker(alwaysPicking(0)).pick(onlyNaengmyeon, "냉면");

		assertThat(pick.menu()).isEqualTo("냉면");
	}

	@Test
	void 실제_난수로도_후보_안의_메뉴만_고른다() {
		MenuPicker picker = new MenuPicker();

		for (int i = 0; i < 100; i++) {
			assertThat(picker.pick(CANDIDATES, "냉면").menu()).isIn("돈까스", "국밥");
		}
	}

	@Test
	void 후보가_없으면_고를_수_없다() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MenuPicker().pick(MenuCandidates.from(List.of()), null));
	}

}
