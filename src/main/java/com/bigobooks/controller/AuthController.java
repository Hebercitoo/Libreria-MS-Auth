package com.bigobooks.controller;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.AuthResponse;
import com.bigobooks.dto.Envelope;
import com.bigobooks.dto.LoginDto;
import com.bigobooks.dto.RegisterDto;
import com.bigobooks.dto.UserRolesRequestDto;
import com.bigobooks.services.AuthService;
import com.bigobooks.util.Envelopes;

/**
 * Controlador de autenticacion: implementa la interfaz AuthApi generada desde
 * openapi.yaml. El login devuelve el token en el cuerpo y ademas lo deja en la
 * cookie "token" para el front-end.
 */
@RestController
public class AuthController implements AuthApi {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private static final String TOKEN_COOKIE = "token";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public ResponseEntity<Envelope> registerUser(RegisterDto registerDto) {
		log.info("POST /auth/register: registro de usuario recibido");
		return ResponseEntity.status(HttpStatus.CREATED).body(Envelopes.single(authService.register(registerDto)));
	}

	@Override
	@PreAuthorize("@roles.allows('user-roles-manage')")
	public ResponseEntity<Envelope> assignUserRoles(Long userId, UserRolesRequestDto userRolesRequestDto) {
		log.info("PUT /auth/users/{}/roles: asignacion de roles recibida", userId);
		return ResponseEntity.ok(Envelopes.single(authService.assignRoles(userId, userRolesRequestDto)));
	}

	@Override
	public ResponseEntity<Envelope> loginUser(LoginDto loginDto) {
		log.info("POST /auth/login: login recibido");
		AuthResponse response = authService.login(loginDto);
		ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE, response.getToken())
				.httpOnly(false)
				.secure(false)
				.path("/")
				.sameSite("Lax")
				.maxAge(Duration.ofSeconds(response.getExpiresIn()))
				.build();
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(Envelopes.single(response));
	}
}
