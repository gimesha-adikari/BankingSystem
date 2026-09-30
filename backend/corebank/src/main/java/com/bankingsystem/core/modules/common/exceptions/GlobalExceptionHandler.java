package com.bankingsystem.core.modules.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.bankingsystem.core.features.transactions.idempotency.domain.*;
import com.bankingsystem.core.features.transactions.retry.FinancialTransactionRetryExhaustedException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<?> handleIdempotencyConflict(IdempotencyConflictException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", ex.getCode(), "message", "Idempotency key was used with a different request")); }
    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<?> handleIdempotencyProgress(IdempotencyInProgressException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", ex.getCode(), "message", "A request with this idempotency key is still processing")); }
    @ExceptionHandler(FinancialTransactionRetryExhaustedException.class)
    public ResponseEntity<?> handleRetryExhausted(FinancialTransactionRetryExhaustedException ex) { return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("code", "ERR_TRANSACTION_TEMPORARILY_UNAVAILABLE", "message", "The transaction is temporarily unavailable")); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadArgument(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("code", "ERR_VALIDATION", "message", "Invalid request")); }

    // Bean Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "ERR_VALIDATION");
        body.put("message", "Validation failed");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
            String message = err.getDefaultMessage();
            errors.put(field, message);
        });
        body.put("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }

    // Domain/business rule violations
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // Not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "ERR_NOT_FOUND");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // Spring Security access denial (PreAuthorize / method security)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "ERR_FORBIDDEN");
        body.put("message", "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Spring Security 6.x AuthorizationDeniedException
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "ERR_FORBIDDEN");
        body.put("message", "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // Conflict (e.g. Idempotency payload mismatch)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "ERR_CONFLICT");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
