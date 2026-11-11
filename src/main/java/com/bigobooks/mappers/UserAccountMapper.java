package com.bigobooks.mappers;

import java.util.List;
import java.util.Set;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bigobooks.dto.UserAccountDto;
import com.bigobooks.entities.auth.Role;
import com.bigobooks.entities.auth.UserAccount;

@Mapper(componentModel = "spring")
public interface UserAccountMapper {

	@BeanMapping(ignoreByDefault = true)
	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "lastName", target = "lastName")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "city", target = "city")
	@Mapping(source = "warehouseId", target = "warehouseId")
	@Mapping(source = "createdAt", target = "createdAt")
	@Mapping(source = "roles", target = "roleIds")
	@Mapping(source = "roles", target = "roleNames")
	UserAccountDto toDto(UserAccount user);

	default List<Long> toRoleIds(Set<Role> roles) {
		return roles == null ? List.of() : roles.stream().map(Role::getId).sorted().toList();
	}

	default List<String> toRoleNames(Set<Role> roles) {
		return roles == null ? List.of() : roles.stream().map(Role::getName).sorted().toList();
	}
}
