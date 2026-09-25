package com.smarttransit.smart_transit.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

	List<Passenger> findByUserIdOrderByFullName(Long userId);

	Optional<Passenger> findByIdAndUserId(Long id, Long userId);
}
