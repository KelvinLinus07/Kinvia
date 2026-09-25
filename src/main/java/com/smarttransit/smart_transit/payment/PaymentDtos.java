package com.smarttransit.smart_transit.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface PaymentDtos {

	record PaymentRequest(@NotNull PaymentMethod method, @NotBlank @Size(max = 64) String instrument) {
	}
}
