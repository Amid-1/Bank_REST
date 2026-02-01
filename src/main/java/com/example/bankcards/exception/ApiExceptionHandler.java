package com.example.bankcards.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        List<ApiErrorResponse.Violation> violations = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return new ApiErrorResponse.Violation(fe.getField(), err.getDefaultMessage());
                    }
                    return new ApiErrorResponse.Violation(err.getObjectName(), err.getDefaultMessage());
                })
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, violations, e, false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.Violation> violations = e.getConstraintViolations()
                .stream()
                .map(v -> new ApiErrorResponse.Violation(extractLeaf(v), v.getMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, violations, e, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "Некорректный JSON в теле запроса", request, null, e, false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        String field = e.getName();
        String msg = "Некорректный формат параметра";
        List<ApiErrorResponse.Violation> violations = List.of(new ApiErrorResponse.Violation(field, msg));
        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, violations, e, false);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.Violation> violations = List.of(
                new ApiErrorResponse.Violation(e.getParameterName(), "Параметр обязателен")
        );
        return build(HttpStatus.BAD_REQUEST, MSG_VALIDATION, request, violations, e, false);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, safeMsg(e, "Не найдено"), request, null, e, false);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "Доступ запрещен", request, null, e, false);
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
        return build(HttpStatus.BAD_REQUEST, safeMsg(e, "Некорректные входные данные"), request, null, e, false);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, safeMsg(e, "Конфликт состояния"), request, null, e, false);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, "Конфликт данных", request, null, e, false);
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
            List<ApiErrorResponse.Violation> violations,
            Exception e,
            boolean logAsError
    ) {
        if (logAsError) {
            log.error("API error {} {}: {}", status.value(), request.getRequestURI(), e.getMessage(), e);
        } else {
            log.warn("API error {} {}: {}", status.value(), request.getRequestURI(), e.getMessage());
        }

        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.name(),
                message,
                request.getRequestURI(),
                (violations == null || violations.isEmpty()) ? null : violations
        );

        return ResponseEntity.status(status).body(body);
    }

    private static String safeMsg(Exception e, String fallback) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? fallback : msg;
    }

    private static String extractLeaf(ConstraintViolation<?> v) {
        Path path = v.getPropertyPath();
        if (path == null) return "param";

        String leaf = null;
        for (Path.Node node : path) {
            if (node != null && node.getName() != null && !node.getName().isBlank()) {
                leaf = node.getName();
            }
        }
        return (leaf == null) ? "param" : leaf;
    }
}
