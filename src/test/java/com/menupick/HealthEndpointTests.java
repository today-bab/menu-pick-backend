package com.menupick;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest(properties = "kakao.rest-key=test-key")
@AutoConfigureMockMvc
class HealthEndpointTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void k8s_프로브용_헬스_체크를_공개한다() {
		assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
		assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk();
	}

	@Test
	void 헬스_체크_외의_actuator_엔드포인트는_공개하지_않는다() {
		assertThat(mvc.get().uri("/actuator/env")).hasStatus4xxClientError();
	}

}
