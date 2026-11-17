package com.example.productservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public org.springframework.http.ResponseEntity<ApiError> notFound(ResourceNotFoundException exception,
			HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
	}

	@ExceptionHandler(DuplicateCategoryException.class)
	public org.springframework.http.ResponseEntity<ApiError> conflict(DuplicateCategoryException exception,
			HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request);
	}

	@ExceptionHandler(CategoryInUseException.class)
	public org.springframework.http.ResponseEntity<ApiError> categoryInUse(CategoryInUseException exception,
			HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public org.springframework.http.ResponseEntity<ApiError> dataConflict(HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "CONFLICT", "The requested change violates a data constraint", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public org.springframework.http.ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
				.collect(java.util.stream.Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
						(first, ignored) -> first));
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", fields.toString(), request);
	}

	private org.springframework.http.ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
			HttpServletRequest request) {
		return org.springframework.http.ResponseEntity.status(status)
				.body(new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI()));
	}

	public record ApiError(Instant timestamp, int status, String code, String message, String path) {
	}
}
