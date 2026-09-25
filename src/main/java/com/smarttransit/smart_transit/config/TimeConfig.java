package com.smarttransit.smart_transit.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

	/** Single time source so services and tests never call {@code now()} directly. */
	@Bean
	Clock clock(KinviaProperties properties) {
		return Clock.system(properties.zone());
	}
}
