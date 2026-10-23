package com.menupick.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.menupick.domain.menu.controller.MenuPickController;
import com.menupick.domain.menu.service.MenuPickService;

@WebMvcTest(controllers = MenuPickController.class, properties = "app.cors.allowed-origins=https://menu.example.com")
class WebConfigTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private MenuPickService menuPickService;

	@Test
	void 허용한_주소의_요청은_CORS를_허용한다() {
		assertThat(mvc.options().uri("/api/menus/pick")
				.header(HttpHeaders.ORIGIN, "https://menu.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.hasStatusOk()
				.hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://menu.example.com");
	}

	@Test
	void 허용하지_않은_주소의_요청은_거부한다() {
		assertThat(mvc.options().uri("/api/menus/pick")
				.header(HttpHeaders.ORIGIN, "https://evil.example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

}
