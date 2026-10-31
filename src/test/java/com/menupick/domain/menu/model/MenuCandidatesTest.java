package com.menupick.domain.menu.model;

import static org.assertj.core.api.Assertions.assertThat;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurant;
import static com.menupick.domain.restaurant.model.TestRestaurants.restaurantOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.menupick.domain.restaurant.model.Restaurant;

class MenuCandidatesTest {

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
		assertThat(MenuCandidates.from(List.of()).categories()).isEmpty();
	}

	@Test
	void 메뉴를_가까운_순서대로_대분류별로_묶는다() {
		MenuCandidates candidates = MenuCandidates.from(List.of(
				restaurantOf("한식", "할매순대국", "국밥"),
				restaurantOf("중식", "홍콩반점", "중식"),
				restaurantOf("한식", "평양면옥", "냉면"),
				restaurantOf("한식", "돼지국밥집", "국밥")));

		assertThat(candidates.categories()).containsExactly(
				new MenuCategory("한식", List.of("국밥", "냉면")),
				new MenuCategory("중식", List.of("중식")));
	}

	@Test
	void 제외한_대분류와_메뉴를_뺀다() {
		Restaurant gukbap = restaurantOf("한식", "할매순대국", "국밥");
		Restaurant naengmyeon = restaurantOf("한식", "평양면옥", "냉면");
		MenuCandidates candidates = MenuCandidates.from(List.of(
				gukbap,
				restaurantOf("중식", "홍콩반점", "중식"),
				naengmyeon,
				restaurantOf("일식", "스시하루", "초밥")));

		MenuCandidates remaining = candidates.excluding(MenuExclusions.of(List.of("중식"), List.of("초밥")));

		assertThat(remaining.menus()).containsExactly("국밥", "냉면");
		assertThat(remaining.restaurantsOf("국밥")).containsExactly(gukbap);
		assertThat(candidates.menus()).hasSize(4);
	}

	@Test
	void 모두_제외하면_후보가_비어_있다() {
		MenuCandidates candidates = MenuCandidates.from(List.of(restaurantOf("한식", "할매순대국", "국밥")));

		assertThat(candidates.excluding(MenuExclusions.of(List.of("한식"), List.of())).isEmpty()).isTrue();
	}

	@Test
	void 제외가_없으면_그대로다() {
		MenuCandidates candidates = MenuCandidates.from(List.of(restaurantOf("한식", "할매순대국", "국밥")));

		assertThat(candidates.excluding(MenuExclusions.NONE).menus()).containsExactly("국밥");
	}

	@Test
	void 여러_대분류에_걸친_메뉴는_가장_가까운_가게의_대분류로_제외한다() {
		MenuCandidates candidates = MenuCandidates.from(List.of(
				restaurantOf("간식", "빵집", "제과,베이커리"),
				restaurantOf("카페", "베이커리카페", "제과,베이커리")));

		assertThat(candidates.excluding(MenuExclusions.of(List.of("간식"), List.of())).isEmpty()).isTrue();
		assertThat(candidates.excluding(MenuExclusions.of(List.of("카페"), List.of())).menus())
				.containsExactly("제과,베이커리");
	}

	@Test
	void 여러_대분류에_걸친_메뉴는_가장_가까운_가게의_대분류에_넣는다() {
		MenuCandidates candidates = MenuCandidates.from(List.of(
				restaurantOf("간식", "빵집", "제과,베이커리"),
				restaurantOf("카페", "베이커리카페", "제과,베이커리")));

		assertThat(candidates.categories()).containsExactly(
				new MenuCategory("간식", List.of("제과,베이커리")));
	}

}
