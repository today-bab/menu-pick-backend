package com.menupick.domain.menu.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class MenuExclusionsTest {

	@Test
	void 대분류를_빼면_그_안의_메뉴가_모두_빠진다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of("한식"), List.of());

		assertThat(exclusions.excludes("한식", "국밥")).isTrue();
		assertThat(exclusions.excludes("한식", "냉면")).isTrue();
		assertThat(exclusions.excludes("중식", "짜장면")).isFalse();
	}

	@Test
	void 대분류를_뺐어도_다시_넣은_메뉴는_남긴다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of("한식"), List.of(), List.of("국밥"));

		assertThat(exclusions.excludes("한식", "국밥")).isFalse();
		assertThat(exclusions.excludes("한식", "냉면")).isTrue();
	}

	@Test
	void 직접_뺀_메뉴는_다시_넣어도_빠진다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of("한식"), List.of("국밥"), List.of("국밥"));

		assertThat(exclusions.excludes("한식", "국밥")).isTrue();
	}

	@Test
	void 대분류를_빼지_않았으면_다시_넣은_메뉴는_영향이_없다() {
		MenuExclusions exclusions = MenuExclusions.of(List.of(), List.of(), List.of("국밥"));

		assertThat(exclusions.excludes("한식", "국밥")).isFalse();
		assertThat(exclusions.excludes("한식", "냉면")).isFalse();
	}

}
