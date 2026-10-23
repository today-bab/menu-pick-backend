package com.menupick.domain.restaurant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.menupick.domain.restaurant.client.KakaoRestaurantClient;
import com.menupick.domain.restaurant.client.RestaurantClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KakaoProperties.class)
public class RestaurantClientConfig {

	@Bean
	RestaurantClient restaurantClient(RestClient.Builder builder, KakaoProperties properties) {
		HttpClientSettings settings = HttpClientSettings.defaults()
				.withTimeouts(properties.connectTimeout(), properties.readTimeout());
		RestClient restClient = builder
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restKey())
				.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
				.build();
		return new KakaoRestaurantClient(restClient, properties);
	}

}
