package com.bigobooks.services;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.AuthResponse;
import com.bigobooks.dto.LoginDto;
import com.bigobooks.dto.RegisterDto;
import com.bigobooks.dto.RoleDto;
import com.bigobooks.dto.UserAccountDto;
import com.bigobooks.dto.UserCreateRequest;
import com.bigobooks.dto.UserRolesRequestDto;
import com.bigobooks.dto.UserUpdateRequest;
import com.bigobooks.entities.auth.Role;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.mappers.UserAccountMapper;
import com.bigobooks.repositories.RoleRepository;
import com.bigobooks.repositories.UserAccountRepository;
import com.bigobooks.config.JwtService;
import com.bigobooks.util.exception.ConflictException;
import com.bigobooks.util.exception.NotFoundException;
import com.bigobooks.util.exception.ResourceType;
import com.bigobooks.util.exception.UnauthorizedException;

/**
 * Registro y autenticacion de usuarios. Los usuarios nuevos se crean con el rol
 * por defecto configurable ({@code security.auth.default-role}); si el rol no
 * existe todavia se crea (la tabla de roles parte vacia).
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserAccountRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final UserAccountMapper userAccountMapper;
	private final String defaultRoleName;

	public AuthService(UserAccountRepository userRepository, RoleRepository roleRepository,
			PasswordEncoder passwordEncoder, JwtService jwtService, UserAccountMapper userAccountMapper,
			@Value("${security.auth.default-role:CLIENT}") String defaultRoleName) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.userAccountMapper = userAccountMapper;
		this.defaultRoleName = defaultRoleName;
	}

	@Transactional
	public UserAccountDto register(RegisterDto request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del usuario");
		}
		String email = requireText(request.getEmail(), "email").toLowerCase();
		String password = requireText(request.getPassword(), "password");
		if (password.length() < 6) {
			throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres");
		}
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new ConflictException("Ya existe un usuario con el email " + email);
		}

		UserAccount user = new UserAccount();
		user.setName(requireText(request.getName(), "name"));
		user.setLastName(requireText(request.getLastName(), "lastName"));
		user.setEmail(email);
		user.setCity(requireText(request.getCity(), "city"));
		user.setWarehouseId(request.getWarehouseId());
		user.setPasswordHash(passwordEncoder.encode(password));
		user.getRoles().add(resolveDefaultRole());

		UserAccount saved = userRepository.save(user);
		log.info("Usuario {} registrado con roles {}", saved.getEmail(), roleNames(saved.getRoles()));
		return userAccountMapper.toDto(saved);
	}

	@Transactional
	public UserAccountDto assignRoles(Long userId, UserRolesRequestDto request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los roles del usuario");
		}

		UserAccount user = userRepository.findById(userId)
				.orElseThrow(() -> NotFoundException.byId(ResourceType.USER, userId));
		user.setRoles(resolveRoles(request.getRoleIds()));
		UserAccount saved = userRepository.save(user);
		log.info("Roles actualizados para usuario {}: {}", saved.getEmail(), roleNames(saved.getRoles()));
		return userAccountMapper.toDto(saved);
	}

	@Transactional(readOnly = true)
	public Page<UserAccountDto> listUsers(String search, Integer page, Integer size) {
		int pageNumber = page == null ? 0 : Math.max(0, page);
		int pageSize = size == null ? 10 : Math.max(1, Math.min(size, 100));
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		PageRequest pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<UserAccount> users = normalizedSearch == null
				? userRepository.findAll(pageable)
				: userRepository.search(normalizedSearch, pageable);
		return users.map(userAccountMapper::toDto);
	}

	@Transactional(readOnly = true)
	public UserAccountDto getUser(Long userId) {
		return userAccountMapper.toDto(findUser(userId));
	}

	@Transactional(readOnly = true)
	public List<RoleDto> listRoles() {
		return roleRepository.findAll(Sort.by("name")).stream().map(role -> {
			RoleDto dto = new RoleDto();
			dto.setId(role.getId());
			dto.setName(role.getName());
			dto.setCreatedAt(role.getCreatedAt());
			return dto;
		}).toList();
	}

	@Transactional
	public UserAccountDto createUser(UserCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del usuario");
		}
		String email = requireText(request.getEmail(), "email").toLowerCase();
		String password = requireText(request.getPassword(), "password");
		validatePassword(password);
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new ConflictException("Ya existe un usuario con el email " + email);
		}

		UserAccount user = new UserAccount();
		user.setName(requireText(request.getName(), "name"));
		user.setLastName(requireText(request.getLastName(), "lastName"));
		user.setEmail(email);
		user.setCity(requireText(request.getCity(), "city"));
		user.setWarehouseId(request.getWarehouseId());
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRoles(resolveRoles(request.getRoleIds()));

		UserAccount saved = userRepository.save(user);
		log.info("Usuario {} creado por administracion con roles {}", saved.getEmail(), roleNames(saved.getRoles()));
		return userAccountMapper.toDto(saved);
	}

	@Transactional
	public UserAccountDto updateUser(Long userId, UserUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos a actualizar");
		}
		UserAccount user = findUser(userId);
		if (request.getName() != null) {
			user.setName(requireText(request.getName(), "name"));
		}
		if (request.getLastName() != null) {
			user.setLastName(requireText(request.getLastName(), "lastName"));
		}
		if (request.getCity() != null) {
			user.setCity(requireText(request.getCity(), "city"));
		}
		if (request.getWarehouseId() != null) {
			user.setWarehouseId(request.getWarehouseId());
		}
		if (request.getEmail() != null) {
			String email = requireText(request.getEmail(), "email").toLowerCase();
			userRepository.findByEmailIgnoreCase(email)
					.filter(other -> !other.getId().equals(userId))
					.ifPresent(other -> {
						throw new ConflictException("Ya existe un usuario con el email " + email);
					});
			user.setEmail(email);
		}
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			validatePassword(request.getPassword());
			user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		}
		if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
			user.setRoles(resolveRoles(request.getRoleIds()));
		}

		UserAccount saved = userRepository.save(user);
		log.info("Usuario {} actualizado por administracion", saved.getEmail());
		return userAccountMapper.toDto(saved);
	}

	@Transactional
	public void deleteUser(Long userId, String currentEmail) {
		UserAccount user = findUser(userId);
		if (currentEmail != null && user.getEmail().equalsIgnoreCase(currentEmail)) {
			throw new ConflictException("No puedes eliminar tu propio usuario");
		}
		userRepository.delete(user);
		log.info("Usuario {} eliminado logicamente por administracion", user.getEmail());
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginDto request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar email y password");
		}
		String email = requireText(request.getEmail(), "email").toLowerCase();
		String password = requireText(request.getPassword(), "password");

		UserAccount user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UnauthorizedException("Credenciales invalidas"));
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new UnauthorizedException("Credenciales invalidas");
		}

		AuthResponse response = new AuthResponse();
		response.setToken(jwtService.generateToken(user));
		response.setTokenType("Bearer");
		response.setExpiresIn(jwtService.getExpirationSeconds());
		response.setUser(userAccountMapper.toDto(user));
		log.info("Login correcto de {}", user.getEmail());
		return response;
	}

	private Role resolveDefaultRole() {
		return roleRepository.findByNameIgnoreCase(defaultRoleName).orElseGet(() -> {
			Role role = new Role();
			role.setName(defaultRoleName);
			Role saved = roleRepository.save(role);
			log.info("Rol por defecto {} creado", saved.getName());
			return saved;
		});
	}

	private Set<Role> resolveRoles(Collection<Long> roleIds) {
		if (roleIds == null || roleIds.isEmpty()) {
			throw new IllegalArgumentException("Debe asignar al menos un rol");
		}

		Set<Long> uniqueIds = new LinkedHashSet<>(roleIds);
		if (uniqueIds.contains(null)) {
			throw new IllegalArgumentException("Los ids de rol no pueden ser nulos");
		}
		if (uniqueIds.size() != roleIds.size()) {
			throw new IllegalArgumentException("No se pueden repetir ids de rol");
		}

		List<Role> roles = roleRepository.findAllById(uniqueIds);
		if (roles.size() != uniqueIds.size()) {
			Set<Long> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
			Long missingId = uniqueIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
			throw NotFoundException.byId(ResourceType.ROLE, missingId);
		}
		return new LinkedHashSet<>(roles);
	}

	private List<String> roleNames(Set<Role> roles) {
		return roles.stream().map(Role::getName).sorted().toList();
	}

	private UserAccount findUser(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("El id del usuario es obligatorio");
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> NotFoundException.byId(ResourceType.USER, userId));
	}

	private void validatePassword(String password) {
		if (password.length() < 6) {
			throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres");
		}
	}

	private String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " es obligatorio");
		}
		return value.trim();
	}
}
