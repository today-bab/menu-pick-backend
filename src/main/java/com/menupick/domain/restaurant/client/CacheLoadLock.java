package com.menupick.domain.restaurant.client;

import org.jspecify.annotations.Nullable;

/**
 * 같은 캐시 키를 여러 요청(여러 인스턴스)이 동시에 채우지 않도록, 한 곳만 원본을 조회하게 하는 락.
 */
public interface CacheLoadLock {

	/** 항상 잡히는 락. 락 없이 요청마다 바로 원본을 조회하게 한다 (단일 인스턴스, 테스트). */
	CacheLoadLock NONE = new CacheLoadLock() {
		@Override
		public String tryAcquire(Object key) {
			return "none";
		}

		@Override
		public void release(Object key, String token) {
		}
	};

	/**
	 * @return 락을 잡았으면 풀 때 쓸 토큰, 다른 곳이 잡고 있으면 {@code null}
	 */
	@Nullable String tryAcquire(Object key);

	/**
	 * 이 토큰으로 잡은 락만 푼다. 이미 만료돼 다른 곳이 잡은 락은 건드리지 않는다.
	 */
	void release(Object key, String token);

}
