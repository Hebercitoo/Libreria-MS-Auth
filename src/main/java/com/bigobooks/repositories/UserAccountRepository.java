package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.repository.BaseRepository;

public interface UserAccountRepository extends BaseRepository<UserAccount> {

	Optional<UserAccount> findByEmailIgnoreCase(String email);

	@Query("select u from UserAccount u where lower(u.name) like lower(concat('%', :search, '%')) "
			+ "or lower(u.lastName) like lower(concat('%', :search, '%')) "
			+ "or lower(u.email) like lower(concat('%', :search, '%'))")
	Page<UserAccount> search(@Param("search") String search, Pageable pageable);

	@Override
	@Query(value = "SELECT * FROM user_account", nativeQuery = true)
	List<UserAccount> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM user_account WHERE is_deleted = true", nativeQuery = true)
	List<UserAccount> findDeleted();
}
