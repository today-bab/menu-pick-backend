package com.menupick.domain.menu.controller;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.menupick.domain.menu.dto.MenuPickResponse;
import com.menupick.domain.menu.service.MenuPickService;
import com.menupick.domain.restaurant.model.Coordinate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@RestController
@RequestMapping("/api/menus")
public class MenuPickController {

	private final MenuPickService menuPickService;

	public MenuPickController(MenuPickService menuPickService) {
		this.menuPickService = menuPickService;
	}

	/**
	 * 현재 위치 근처 음식점에서 메뉴를 하나 뽑는다.
	 *
	 * @param exclude 직전에 뽑은 메뉴
	 */
	@GetMapping("/pick")
	public MenuPickResponse pick(
			@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
			@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
			@RequestParam(required = false) @Nullable String exclude) {
		return MenuPickResponse.from(menuPickService.pick(new Coordinate(lat, lng), exclude));
	}

}
