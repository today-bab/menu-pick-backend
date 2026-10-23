package com.menupick.domain.menu.model;

import java.util.List;

import com.menupick.domain.restaurant.model.Restaurant;

/**
 * 뽑힌 메뉴와 그 메뉴를 파는 근처 음식점 (가까운 순).
 */
public record MenuPick(String menu, List<Restaurant> restaurants) {
}
