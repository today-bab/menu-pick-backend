package com.menupick.domain.menu.model;

import java.util.List;

/**
 * 대분류 하나와 그 아래 근처에서 뽑을 수 있는 메뉴들.
 *
 * @param name 대분류 (예: 한식)
 * @param menus 메뉴 (예: 국밥, 냉면). 가장 가까운 가게가 먼저 나온 메뉴부터.
 */
public record MenuCategory(String name, List<String> menus) {

	public MenuCategory {
		menus = List.copyOf(menus);
	}

}
