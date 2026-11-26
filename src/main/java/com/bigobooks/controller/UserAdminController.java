package com.bigobooks.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.Envelope;
import com.bigobooks.dto.UserAccountDto;
import com.bigobooks.dto.UserCreateRequest;
import com.bigobooks.dto.UserUpdateRequest;
import com.bigobooks.services.AuthService;
import com.bigobooks.util.Envelopes;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class UserAdminController {

    private static final Logger log = LoggerFactory.getLogger(UserAdminController.class);

    private final AuthService authService;

    public UserAdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    @PreAuthorize("@roles.allows('users-view')")
    public ResponseEntity<Envelope> listUsers(@RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        Page<UserAccountDto> users = authService.listUsers(search, page, size);
        return ResponseEntity.ok(Envelopes.page(users, users.getContent()));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("@roles.allows('users-view')")
    public ResponseEntity<Envelope> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(Envelopes.single(authService.getUser(userId)));
    }

    @PostMapping("/users")
    @PreAuthorize("@roles.allows('users-manage')")
    public ResponseEntity<Envelope> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("POST /auth/users: creacion administrativa recibida");
        return ResponseEntity.status(HttpStatus.CREATED).body(Envelopes.single(authService.createUser(request)));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("@roles.allows('users-manage')")
    public ResponseEntity<Envelope> updateUser(@PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /auth/users/{}: actualizacion administrativa recibida", userId);
        return ResponseEntity.ok(Envelopes.single(authService.updateUser(userId, request)));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("@roles.allows('users-manage')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication == null ? null : authentication.getName();
        log.info("DELETE /auth/users/{}: borrado logico recibido", userId);
        authService.deleteUser(userId, currentEmail);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    @PreAuthorize("@roles.allows('users-view')")
    public ResponseEntity<Envelope> listRoles() {
        return ResponseEntity.ok(Envelopes.single(authService.listRoles()));
    }
}
