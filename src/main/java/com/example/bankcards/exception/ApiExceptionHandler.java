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

    private static final String MSG_VALIDATION = "Ошибка валидации";
    private static final String MSG_BAD_CREDENTIALS = "Неверный логин или пароль";

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

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, fields, e, false);
    }

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

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, fields, e, false);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request, null, e, false);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        String msg = (e.getMessage() == null || e.getMessage().isBlank())
                ? "Доступ запрещен"
                : e.getMessage();

        return build(HttpStatus.FORBIDDEN, msg, request, null, e, false);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, MSG_BAD_CREDENTIALS, request, null, e, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        String msg = (e.getMessage() == null || e.getMessage().isBlank())
                ? "Некорректные входные данные"
                : e.getMessage();

        return build(HttpStatus.BAD_REQUEST, msg, request, null, e, false);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException e,
            HttpServletRequest request
    ) {
        String msg = (e.getMessage() == null || e.getMessage().isBlank())
                ? "Конфликт состояния"
                : e.getMessage();

        return build(HttpStatus.CONFLICT, msg, request, null, e, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAny(
            Exception e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, null, e, true);
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
