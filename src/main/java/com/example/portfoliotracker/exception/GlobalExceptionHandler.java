package com.example.portfoliotracker.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        ApiError error = new ApiError();
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setError("Not Found");
        error.setMessage(ex.getMessage());
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle validation errors from @Valid annotation
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ApiError error = new ApiError();
        error.setStatus(status.value());
        error.setError("Validation Failed");
        error.setMessage("Request validation failed");
        error.setPath(request.getDescription(false).replace("uri=", ""));
        error.setDetails(ex.getBindingResult().getFieldErrors()
            .stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList()));

        log.warn("Validation error: {}", error.getMessage());
        return ResponseEntity.status(status.value()).body(error);
    }

    /**
     * Handle type mismatch errors (e.g., enum conversion failures)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        ApiError error = new ApiError();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("Bad Request");

        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String attempted = ex.getValue() != null ? ex.getValue().toString() : "null";

        String message;
        // If the required type is an enum, list allowed values
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            Class<?> enumType = ex.getRequiredType();
            String allowed = Arrays.stream(enumType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = String.format(
                    "Argument [%s] is not a valid %s. Allowed values: [%s]",
                    attempted, requiredType, allowed
            );
        } else {
            message = String.format(
                    "Argument [%s] of type [%s] did not match parameter type [%s]",
                    attempted,
                    ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null",
                    requiredType
            );
        }

        error.setMessage(message);
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.warn("Type mismatch error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle JSON parsing errors (malformed request body)
     *
     * NOTE: Override the ResponseEntityExceptionHandler's method instead of using
     * @ExceptionHandler to avoid ambiguous handler mappings.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ApiError error = new ApiError();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("Bad Request");
        error.setMessage("Invalid or malformed JSON in request body");
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.warn("JSON parsing error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        ApiError error = new ApiError();
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setError("Bad Request");
        error.setMessage(ex.getMessage());
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.warn("Illegal argument error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle database/SQL errors
     */
    @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(
            org.hibernate.exception.ConstraintViolationException ex, WebRequest request) {

        ApiError error = new ApiError();
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setError("Constraint Violation");

        String message = ex.getMessage();
        if (message != null && message.contains("cannot be null")) {
            error.setMessage("Required field cannot be null");
        } else if (message != null && message.contains("unique")) {
            error.setMessage("Duplicate entry violates unique constraint");
        } else {
            error.setMessage("Database constraint violated");
        }
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.error("Database constraint violation: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle DataIntegrityViolationException (wrapper for constraint violations)
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {

        ApiError error = new ApiError();
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setError("Data Integrity Violation");

        String message = ex.getMessage();
        if (message != null && message.contains("cannot be null")) {
            error.setMessage("Required field cannot be null");
        } else if (message != null && message.contains("unique")) {
            error.setMessage("Duplicate entry violates unique constraint");
        } else {
            error.setMessage("Data integrity constraint violated");
        }
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneric(Exception ex, WebRequest request) {
        ApiError error = new ApiError();
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setError("Internal Server Error");
        error.setMessage(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        error.setPath(request.getDescription(false).replace("uri=", ""));

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
