package com.example.bankcards.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    // 400 — DTO body validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldErrorItem> fields = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiErrorResponse.FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fields, e, false);
    }

    // 400 — validation on params/path vars
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldErrorItem> fields = e.getConstraintViolations()
                .stream()
                .map(v -> new ApiErrorResponse.FieldErrorItem(
                        v.getPropertyPath() != null ? v.getPropertyPath().toString() : "param",
                        v.getMessage()
                ))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Constraint violation", request, fields, e, false);
    }

    // 404 — “не найдено”
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request, null, e, false);
    }

    // 403 — нет прав
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, e.getMessage(), request, null, e, false);
    }

    // 401 — неверный логин/пароль
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, "Bad credentials", request, null, e, false);
    }

    // 400 — неверные входные данные (например last4 не 4 цифры)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request, null, e, false);
    }

    // 409 — конфликт по бизнес-состоянию (карта не ACTIVE, просрочена, недостаточно средств и т.д.)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, e.getMessage(), request, null, e, false);
    }

    // 500 — все остальное
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAny(
            Exception e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", request, null, e, true);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ApiErrorResponse.FieldErrorItem> fieldErrors,
            Exception e,
            boolean logAsError
    ) {
        if (logAsError) {
            log.error("API error {} {}: {}", status.value(), request.getRequestURI(), e.getMessage(), e);
        } else {
            log.warn("API error {} {}: {}", status.value(), request.getRequestURI(), e.getMessage());
        }

        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.name(),
                message,
                request.getRequestURI(),
                (fieldErrors == null || fieldErrors.isEmpty()) ? null : fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
