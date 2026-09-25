package com.smarttransit.smart_transit.notification;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@EntityGraph(attributePaths = "booking")
	Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = "booking")
	Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

	long countByUserIdAndReadFalse(Long userId);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	@Modifying
	@Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
	int markAllRead(@Param("userId") Long userId);
}
