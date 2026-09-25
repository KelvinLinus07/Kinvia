package com.smarttransit.smart_transit.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	@Query("select u from User u left join fetch u.operator where u.id = :id")
	Optional<User> findWithOperatorById(@Param("id") Long id);

	@Query(value = """
			select u from User u left join fetch u.operator
			where lower(u.email) like concat('%', :q, '%') or lower(u.fullName) like concat('%', :q, '%')
			""", countQuery = """
			select count(u) from User u
			where lower(u.email) like concat('%', :q, '%') or lower(u.fullName) like concat('%', :q, '%')
			""")
	Page<User> search(@Param("q") String query, Pageable pageable);

	long countByRole(Role role);
}
