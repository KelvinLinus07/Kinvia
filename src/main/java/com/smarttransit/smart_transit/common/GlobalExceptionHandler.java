package com.smarttransit.smart_transit.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.persistence.OptimisticLockException;

/** Renders every failure, including Spring MVC's own, as the same {@link ApiError} shape. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<Object> handleApi(ApiException ex, WebRequest request) {
		return build(ex.status(), ex.getMessage(), request, Map.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'", request, Map.of());
	}

	@ExceptionHandler({ DataIntegrityViolationException.class, OptimisticLockException.class })
	ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
		log.warn("Data conflict: {}", ex.getMessage());
		return build(HttpStatus.CONFLICT, "The request conflicts with existing data. Please refresh and retry.",
				request, Map.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side.", request, Map.of());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
		return build(HttpStatus.BAD_REQUEST, "Some fields are invalid", request, fields);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		return build(HttpStatus.valueOf(statusCode.value()), ex.getMessage(), request, Map.of());
	}

	private ResponseEntity<Object> build(HttpStatus status, String message, WebRequest request,
			Map<String, String> fields) {
		String path = request instanceof ServletWebRequest web ? web.getRequest().getRequestURI() : "";
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fields);
		return ResponseEntity.status(status).body(body);
	}
}
