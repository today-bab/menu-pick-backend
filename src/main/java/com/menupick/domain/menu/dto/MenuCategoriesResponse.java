package com.menupick.domain.menu.dto;

import java.util.List;

import com.menupick.domain.menu.model.MenuCategory;

/**
 * 근처 메뉴 카테고리 응답. 대분류와 메뉴는 가장 가까운 가게가 먼저 나온 순서다.
 */
public record MenuCategoriesResponse(List<CategoryResponse> categories) {

	public record CategoryResponse(String name, List<String> menus) {

		static CategoryResponse from(MenuCategory category) {
			return new CategoryResponse(category.name(), category.menus());
		}

	}

	public static MenuCategoriesResponse from(List<MenuCategory> categories) {
		return new MenuCategoriesResponse(categories.stream().map(CategoryResponse::from).toList());
	}

}
