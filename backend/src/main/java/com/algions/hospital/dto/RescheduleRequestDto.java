package com.algions.hospital.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record RescheduleRequestDto(
    @NotNull(message = "New appointment time is required")
    @Future(message = "New appointment time must be in the future")
    OffsetDateTime newAppointmentTime
) {}
