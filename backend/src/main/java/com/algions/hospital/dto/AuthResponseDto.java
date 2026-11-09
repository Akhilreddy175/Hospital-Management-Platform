package com.algions.hospital.dto;

public record AuthResponseDto(
    String token,
    String tokenType,
    String email,
    String role,
    long expiresInMs
) {
    public AuthResponseDto(String token, String email, String role, long expiresInMs) {
        this(token, "Bearer", email, role, expiresInMs);
    }
}
