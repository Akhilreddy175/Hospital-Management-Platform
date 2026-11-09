package com.algions.hospital.dto;

import java.time.OffsetDateTime;

public record ErrorResponseDto(
    int status,
    String error,
    String message,
    String path,
    OffsetDateTime timestamp
) {
    public ErrorResponseDto(int status, String error, String message, String path) {
        this(status, error, message, path, OffsetDateTime.now());
    }
}
