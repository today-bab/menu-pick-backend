package com.menupick.domain.menu.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 사용자가 추천에서 빼 달라고 한 대분류와 메뉴. 직전 메뉴와 달리 다른 후보가 없어도 반드시 뺀다.
 *
 * @param cuisines 통째로 뺄 대분류 (예: 중식)
 * @param menus 하나씩 뺄 메뉴 (예: 국밥)
 * @param includedMenus 대분류를 통째로 뺐어도 다시 넣을 메뉴 (예: 한식은 빼되 국밥은 먹고 싶을 때의 국밥)
 */
public record MenuExclusions(Set<String> cuisines, Set<String> menus, Set<String> includedMenus) {

	public static final MenuExclusions NONE = new MenuExclusions(Set.of(), Set.of(), Set.of());

	public MenuExclusions {
		cuisines = Set.copyOf(cuisines);
		menus = Set.copyOf(menus);
		includedMenus = Set.copyOf(includedMenus);
	}

	public static MenuExclusions of(Collection<String> cuisines, Collection<String> menus) {
		return of(cuisines, menus, List.of());
	}

	public static MenuExclusions of(
			Collection<String> cuisines, Collection<String> menus, Collection<String> includedMenus) {
		return new MenuExclusions(Set.copyOf(cuisines), Set.copyOf(menus), Set.copyOf(includedMenus));
	}

	/**
	 * 다시 넣은 메뉴는 대분류 제외만 이긴다. 메뉴를 직접 뺐다면 그쪽이 더 구체적인 뜻이라 그대로 뺀다.
	 */
	public boolean excludes(String cuisine, String menu) {
		boolean cuisineExcluded = cuisines.contains(cuisine) && !includedMenus.contains(menu);
		return cuisineExcluded || menus.contains(menu);
	}

}
