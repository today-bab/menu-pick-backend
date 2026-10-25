package com.menupick;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "kakao.rest-key=test-key", "management.server.port=0" })
// 테스트에서는 지표 내보내기가 기본으로 꺼져 있어 Prometheus 엔드포인트를 확인하려면 켜야 한다
@AutoConfigureMetrics
class HealthEndpointTests {

	@LocalServerPort
	private int serverPort;

	@LocalManagementPort
	private int managementPort;

	private RestClient client;

	@BeforeEach
	void setUp() {
		client = RestClient.builder()
				.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
				})
				.build();
	}

	private HttpStatusCode statusOf(int port, String path) {
		return client.get().uri("http://localhost:" + port + path).retrieve().toBodilessEntity().getStatusCode();
	}

	@Test
	void 관리_포트에서_k8s_프로브용_헬스_체크를_연다() {
		assertThat(statusOf(managementPort, "/actuator/health/liveness").value()).isEqualTo(200);
		assertThat(statusOf(managementPort, "/actuator/health/readiness").value()).isEqualTo(200);
	}

	@Test
	void 관리_포트에서_Prometheus_지표를_연다() {
		String metrics = client.get()
				.uri("http://localhost:" + managementPort + "/actuator/prometheus")
				.retrieve()
				.body(String.class);

		assertThat(metrics).contains("jvm_memory_used_bytes", "cache_gets_total", "restaurant_cache_errors_total",
				"application=\"menu-pick-backend\"");
	}

	@Test
	void 서비스_포트에는_actuator를_열지_않는다() {
		assertThat(statusOf(serverPort, "/actuator/health").value()).isEqualTo(404);
		assertThat(statusOf(serverPort, "/actuator/prometheus").value()).isEqualTo(404);
	}

	@Test
	void 관리_포트에서도_헬스_체크와_지표_외에는_열지_않는다() {
		assertThat(statusOf(managementPort, "/actuator/env").is4xxClientError()).isTrue();
	}

}
