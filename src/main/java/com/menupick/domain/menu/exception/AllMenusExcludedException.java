package com.menupick.domain.menu.exception;

import com.menupick.domain.menu.model.MenuExclusions;

/**
 * 근처에 음식점은 있지만 사용자가 제외한 대분류·메뉴를 빼면 뽑을 메뉴가 남지 않을 때 던진다.
 */
public class AllMenusExcludedException extends RuntimeException {

	public AllMenusExcludedException(MenuExclusions exclusions) {
		super("제외한 메뉴를 빼면 뽑을 메뉴가 없습니다: " + exclusions);
	}

}
