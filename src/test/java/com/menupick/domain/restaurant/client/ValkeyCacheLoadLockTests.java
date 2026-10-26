package com.menupick.domain.restaurant.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 Valkey 컨테이너로 락을 확인한다. Docker가 없으면 건너뛴다.
 */
@SpringBootTest(properties = "kakao.rest-key=test-key")
@Testcontainers(disabledWithoutDocker = true)
class ValkeyCacheLoadLockTests {

	@Container
	@ServiceConnection(name = "redis")
	static final GenericContainer<?> valkey = new GenericContainer<>("valkey/valkey:9.1.2-alpine")
			.withExposedPorts(6379);

	private static final String KEY = "칸";

	@Autowired
	private StringRedisTemplate redis;

	private ValkeyCacheLoadLock lock;

	@BeforeEach
	void setUp() {
		lock = new ValkeyCacheLoadLock(redis, "test:lock::", Duration.ofSeconds(10));
		redis.delete(lock.lockKey(KEY));
	}

	@Test
	void 먼저_잡은_한_곳만_락을_얻는다() {
		String first = lock.tryAcquire(KEY);
		String second = lock.tryAcquire(KEY);

		assertThat(first).isNotNull();
		assertThat(second).isNull();
	}

	@Test
	void 풀면_다시_잡을_수_있다() {
		String token = lock.tryAcquire(KEY);
		lock.release(KEY, token);

		assertThat(lock.tryAcquire(KEY)).isNotNull();
	}

	@Test
	void 다른_토큰으로는_풀_수_없다() {
		lock.tryAcquire(KEY);
		lock.release(KEY, "다른-토큰");

		assertThat(lock.tryAcquire(KEY)).isNull();
	}

	@Test
	void 락에는_만료_시간이_걸려_잡은_쪽이_죽어도_남지_않는다() {
		lock.tryAcquire(KEY);

		assertThat(redis.getExpire(lock.lockKey(KEY))).isBetween(1L, 10L);
	}

}
