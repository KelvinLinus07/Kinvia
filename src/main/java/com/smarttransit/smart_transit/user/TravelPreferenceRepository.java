package com.smarttransit.smart_transit.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelPreferenceRepository extends JpaRepository<TravelPreference, Long> {

	Optional<TravelPreference> findByUserId(Long userId);
}
