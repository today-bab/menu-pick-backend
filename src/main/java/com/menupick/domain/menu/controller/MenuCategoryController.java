package com.menupick.domain.menu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.menupick.domain.menu.dto.MenuCategoriesResponse;
import com.menupick.domain.menu.service.MenuCategoryService;
import com.menupick.domain.restaurant.model.Coordinate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@RestController
@RequestMapping("/api/menus")
public class MenuCategoryController {

	private final MenuCategoryService menuCategoryService;

	public MenuCategoryController(MenuCategoryService menuCategoryService) {
		this.menuCategoryService = menuCategoryService;
	}

	/**
	 * 현재 위치 근처에서 뽑을 수 있는 메뉴를 대분류별로 돌려준다.
	 * 여기 나온 대분류·메뉴 이름을 뽑기의 {@code excludeCuisine}·{@code excludeMenu}로 보내면 뽑기에서 빠진다.
	 */
	@GetMapping("/categories")
	public MenuCategoriesResponse categories(
			@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
			@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng) {
		return MenuCategoriesResponse.from(menuCategoryService.categories(new Coordinate(lat, lng)));
	}

}
