package com.algions.hospital.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record AppointmentRequestDto(
    @NotNull(message = "Doctor ID is required")
    Long doctorId,

    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment time must be in the future")
    OffsetDateTime appointmentTime,

    String reason
) {}
