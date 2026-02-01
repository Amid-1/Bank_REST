package com.example.bankcards.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Единый формат ошибки API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(

        @Schema(description = "Время ошибки (UTC offset)")
        OffsetDateTime timestamp,

        @Schema(description = "HTTP статус-код", example = "400")
        int status,

        @Schema(description = "Короткое имя статуса", example = "BAD_REQUEST")
        String error,

        @Schema(description = "Сообщение об ошибке", example = "Ошибка валидации")
        String message,

        @Schema(description = "Путь запроса", example = "/api/transfers")
        String path,

        @Schema(description = "Ошибки по полям/параметрам (для валидации)")
        List<Violation> violations
) {
    public record Violation(
            @Schema(description = "Поле/параметр, где ошибка", example = "email")
            String field,
            @Schema(description = "Сообщение об ошибке", example = "Некорректный email")
            String message
    ) {}
}
