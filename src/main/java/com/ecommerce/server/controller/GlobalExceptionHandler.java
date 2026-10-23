package com.ecommerce.server.controller;

import com.ecommerce.server.dto.error.ApiError;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        int status = exception.getStatusCode().value();
        HttpStatus resolvedStatus = HttpStatus.resolve(status);
        String message = exception.getReason() != null
                ? exception.getReason()
                : resolvedStatus == null
                        ? "Request failed"
                        : resolvedStatus.getReasonPhrase();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ApiError(status, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ApiError(
                        HttpStatus.BAD_REQUEST.value(),
                        "Malformed JSON request"
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        HttpStatus.CONFLICT.value(),
                        "Data conflicts with existing records"
                ));
    }
}
