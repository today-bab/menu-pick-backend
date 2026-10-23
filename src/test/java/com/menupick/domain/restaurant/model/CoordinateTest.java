package com.menupick.domain.restaurant.model;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CoordinateTest {

	@Test
	void 범위_안의_좌표는_만들_수_있다() {
		assertThatNoException().isThrownBy(() -> new Coordinate(37.5665, 126.978));
	}

	@ParameterizedTest
	@CsvSource({ "90.1, 0", "-90.1, 0", "0, 180.1", "0, -180.1" })
	void 범위를_벗어난_좌표는_만들_수_없다(double lat, double lng) {
		assertThatIllegalArgumentException().isThrownBy(() -> new Coordinate(lat, lng));
	}

}
