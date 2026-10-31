package com.menupick.domain.menu.model;

import java.util.Collection;
import java.util.Set;

/**
 * 사용자가 추천에서 빼 달라고 한 대분류와 메뉴. 직전 메뉴와 달리 다른 후보가 없어도 반드시 뺀다.
 *
 * @param cuisines 통째로 뺄 대분류 (예: 중식)
 * @param menus 하나씩 뺄 메뉴 (예: 국밥)
 */
public record MenuExclusions(Set<String> cuisines, Set<String> menus) {

	public static final MenuExclusions NONE = new MenuExclusions(Set.of(), Set.of());

	public MenuExclusions {
		cuisines = Set.copyOf(cuisines);
		menus = Set.copyOf(menus);
	}

	public static MenuExclusions of(Collection<String> cuisines, Collection<String> menus) {
		return new MenuExclusions(Set.copyOf(cuisines), Set.copyOf(menus));
	}

	public boolean excludes(String cuisine, String menu) {
		return cuisines.contains(cuisine) || menus.contains(menu);
	}

}
