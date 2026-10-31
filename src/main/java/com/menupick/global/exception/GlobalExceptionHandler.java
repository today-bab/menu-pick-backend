package com.menupick.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.menupick.domain.menu.exception.AllMenusExcludedException;
import com.menupick.domain.menu.exception.NoNearbyRestaurantException;
import com.menupick.domain.restaurant.exception.RestaurantLookupException;

/**
 * 예외를 RFC 9457 ProblemDetail 응답으로 바꾼다.
 * 요청 파라미터 누락·형식·검증 오류(400)는 {@link ResponseEntityExceptionHandler}가 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler
	ProblemDetail handle(NoNearbyRestaurantException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "근처에 음식점이 없습니다.");
	}

	/** 요청 형식은 맞지만 그 조건으로는 뽑을 수 없어 422로 응답한다. 근처에 음식점이 없는 404와 안내를 구분한다. */
	@ExceptionHandler
	ProblemDetail handle(AllMenusExcludedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, "제외한 메뉴를 빼면 근처에 뽑을 메뉴가 없습니다.");
	}

	@ExceptionHandler
	ProblemDetail handle(RestaurantLookupException ex) {
		log.warn("음식점 조회 실패", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "음식점 정보를 불러오지 못했습니다.");
	}

}
