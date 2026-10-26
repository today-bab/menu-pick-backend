package com.menupick.domain.restaurant.client;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Valkey(Redis 호환)의 {@code SET NX PX}로 잡는 {@link CacheLoadLock}.
 * <p>
 * 만료 시간이 있어 락을 잡은 인스턴스가 죽어도 락이 남지 않는다.
 * 풀 때는 내 토큰일 때만 지워, 만료 뒤 다른 곳이 잡은 락을 지우지 않는다.
 */
public class ValkeyCacheLoadLock implements CacheLoadLock {

	private static final RedisScript<Long> RELEASE = RedisScript.of("""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			  return redis.call('del', KEYS[1])
			end
			return 0
			""", Long.class);

	private final StringRedisTemplate redis;
	private final String keyPrefix;
	private final Duration ttl;

	/**
	 * @param keyPrefix 락 키 앞에 붙일 이름 (예: {@code nearby-restaurants:lock::})
	 * @param ttl 락을 잡은 쪽이 풀지 못해도 저절로 풀리는 시간
	 */
	public ValkeyCacheLoadLock(StringRedisTemplate redis, String keyPrefix, Duration ttl) {
		this.redis = redis;
		this.keyPrefix = keyPrefix;
		this.ttl = ttl;
	}

	@Override
	public @Nullable String tryAcquire(Object key) {
		String token = UUID.randomUUID().toString();
		Boolean acquired = redis.opsForValue().setIfAbsent(lockKey(key), token, ttl);
		return Boolean.TRUE.equals(acquired) ? token : null;
	}

	@Override
	public void release(Object key, String token) {
		redis.execute(RELEASE, List.of(lockKey(key)), token);
	}

	String lockKey(Object key) {
		return keyPrefix + key;
	}

}
