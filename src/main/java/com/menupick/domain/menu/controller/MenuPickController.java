package com.menupick.domain.menu.controller;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.menupick.domain.menu.dto.MenuPickResponse;
import com.menupick.domain.menu.model.MenuExclusions;
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
	 * <p>
	 * 제외할 대분류·메뉴는 {@code excludeCuisine=중식&excludeCuisine=일식&excludeMenu=국밥}처럼 같은 이름을 반복해 받는다.
	 * 대분류를 뺐어도 다시 넣을 메뉴는 {@code excludeCuisine=한식&includeMenu=국밥}처럼 받는다.
	 *
	 * @param exclude 직전에 뽑은 메뉴
	 */
	@GetMapping("/pick")
	public MenuPickResponse pick(
			@RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
			@RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
			@RequestParam(required = false) @Nullable String exclude,
			WebRequest request) {
		MenuExclusions exclusions = MenuExclusions.of(
				parameterValues(request, "excludeCuisine"),
				parameterValues(request, "excludeMenu"),
				parameterValues(request, "includeMenu"));
		return MenuPickResponse.from(menuPickService.pick(new Coordinate(lat, lng), exclude, exclusions));
	}

	/**
	 * 받은 값을 그대로 꺼낸다. {@code @RequestParam List<String>}은 값이 하나면 쉼표로 쪼개서,
	 * "육류,고기"·"제과,베이커리"처럼 쉼표가 든 메뉴 이름이 깨진다.
	 */
	private static List<String> parameterValues(WebRequest request, String name) {
		String[] values = request.getParameterValues(name);
		return values == null ? List.of() : List.of(values);
	}

}
