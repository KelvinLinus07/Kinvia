package com.smarttransit.smart_transit.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedJourneyRepository extends JpaRepository<SavedJourney, Long> {

	@EntityGraph(attributePaths = { "origin", "destination" })
	List<SavedJourney> findByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<SavedJourney> findByIdAndUserId(Long id, Long userId);

	boolean existsByUserIdAndOriginIdAndDestinationId(Long userId, Long originId, Long destinationId);
}
