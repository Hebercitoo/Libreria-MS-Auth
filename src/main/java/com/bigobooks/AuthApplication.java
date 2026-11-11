package com.bigobooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Microservicio de autenticacion. Se excluye {@link BigoBooksApplication} (la
 * clase de arranque del common) del scan de componentes: esta en el classpath
 * por el JAR compartido y no debe instanciarse dentro de este servicio.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

}
