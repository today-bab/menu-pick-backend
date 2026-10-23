package com.menupick.domain.restaurant.exception;

/**
 * 외부 서비스에서 음식점을 조회하지 못했을 때 던진다.
 */
public class RestaurantLookupException extends RuntimeException {

	public RestaurantLookupException(String message, Throwable cause) {
		super(message, cause);
	}

}
