package com.menupick.domain.menu.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.menupick.domain.menu.model.MenuCandidates;
import com.menupick.domain.menu.model.MenuPick;

/**
 * 뽑기 후보에서 메뉴 하나를 무작위로 고른다.
 * 직전에 뽑은 메뉴는 다른 후보가 있으면 다시 고르지 않는다.
 */
@Component
public class MenuPicker {

	private final RandomGenerator random;

	/**
	 * 여러 요청이 동시에 쓰므로 스레드마다 따로 동작하는 {@link ThreadLocalRandom}을 쓴다.
	 * ({@code RandomGenerator.getDefault()}는 스레드 안전하지 않고, jdk.random 모듈이 없는 JRE에서는 만들 수도 없다.)
	 */
	public MenuPicker() {
		this(ThreadLocalRandom.current());
	}

	MenuPicker(RandomGenerator random) {
		this.random = random;
	}

	public MenuPick pick(MenuCandidates candidates, @Nullable String excludedMenu) {
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException("뽑을 메뉴 후보가 없습니다");
		}

		List<String> menus = candidates.menus();
		List<String> pickable = menus.stream()
				.filter(menu -> !menu.equals(excludedMenu))
				.toList();
		if (pickable.isEmpty()) {
			pickable = menus;
		}

		String menu = pickable.get(random.nextInt(pickable.size()));
		return new MenuPick(menu, candidates.restaurantsOf(menu));
	}

}
