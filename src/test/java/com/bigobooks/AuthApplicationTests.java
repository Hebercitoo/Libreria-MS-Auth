package com.bigobooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Carga del contexto completo. Requiere que exista
 * src/main/resources/config/private.properties con una base de datos accesible;
 * se ejecuta solo si se define la propiedad de sistema auth.test.full=true.
 */
@SpringBootTest(classes = AuthApplication.class)
@EnabledIfSystemProperty(named = "auth.test.full", matches = "true")
class AuthApplicationTests {

	@Test
	void contextLoads() {
	}

}
