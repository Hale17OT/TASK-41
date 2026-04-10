package com.dispatchops.web.advice;

import com.dispatchops.domain.exception.*;
import com.dispatchops.web.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResult<?>> handleAccountLocked(AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        java.util.Map<String, Object> lockoutData = new java.util.LinkedHashMap<>();
        lockoutData.put("lockoutExpiry", java.time.LocalDateTime.now().plusSeconds(ex.getRemainingSeconds()).toString());
        lockoutData.put("remainingSeconds", ex.getRemainingSeconds());
        ApiResult<java.util.Map<String, Object>> result = ApiResult.success(lockoutData);
        result.setCode(423);
        result.setMessage(ex.getMessage());
        return ResponseEntity.status(423).body(result);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResult<?>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        if (ex.hasRemainingAttempts()) {
            java.util.Map<String, Object> authData = new java.util.LinkedHashMap<>();
            authData.put("remainingAttempts", ex.getRemainingAttempts());
            ApiResult<java.util.Map<String, Object>> result = ApiResult.success(authData);
            result.setCode(401);
            result.setMessage(ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error(401, ex.getMessage()));
    }

    @ExceptionHandler(AddressValidationException.class)
    public ResponseEntity<ApiResult<?>> handleAddressValidation(AddressValidationException ex) {
        log.warn("Address validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(AppealWindowClosedException.class)
    public ResponseEntity<ApiResult<?>> handleAppealWindowClosed(AppealWindowClosedException ex) {
        log.warn("Appeal window closed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(RefundWindowClosedException.class)
    public ResponseEntity<ApiResult<?>> handleRefundWindowClosed(RefundWindowClosedException ex) {
        log.warn("Refund window closed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientCreditException.class)
    public ResponseEntity<ApiResult<?>> handleInsufficientCredit(InsufficientCreditException ex) {
        log.warn("Insufficient credit: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResult<?>> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(StaleTransitionException.class)
    public ResponseEntity<ApiResult<?>> handleStaleTransition(StaleTransitionException ex) {
        log.warn("Stale transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(409, ex.getMessage()));
    }

    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ApiResult<?>> handleIdempotencyViolation(IdempotencyViolationException ex) {
        log.warn("Idempotency violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(409, ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResult<?>> handleOptimisticLock(OptimisticLockException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(409, ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResult<?>> handlePermissionDenied(PermissionDeniedException ex) {
        log.warn("Permission denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error(403, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResult<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<?>> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        List<ApiResult.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiResult.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        ApiResult<?> result = ApiResult.validationError(fieldErrors);
        result.setCode(422);
        result.setMessage("Validation failed");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<?>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResult<?>> handleIllegalState(IllegalStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResult.error(422, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleGeneral(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(500, "An unexpected error occurred. Please try again later."));
    }
}
