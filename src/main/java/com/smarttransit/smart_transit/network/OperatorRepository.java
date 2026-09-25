package com.smarttransit.smart_transit.network;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

	boolean existsByCodeIgnoreCase(String code);

	Optional<Operator> findByCodeIgnoreCase(String code);
}
