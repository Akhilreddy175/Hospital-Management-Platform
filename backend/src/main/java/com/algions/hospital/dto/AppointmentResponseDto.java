package com.algions.hospital.dto;

import com.algions.hospital.entity.Appointment;
import com.algions.hospital.entity.AppointmentStatus;
import java.time.OffsetDateTime;

public record AppointmentResponseDto(
    Long id,
    Long doctorId,
    String doctorName,
    String specialization,
    Long patientId,
    String patientName,
    OffsetDateTime appointmentTime,
    OffsetDateTime endTime,
    AppointmentStatus status,
    String reason,
    OffsetDateTime createdAt
) {
    public static AppointmentResponseDto fromEntity(Appointment appointment) {
        return new AppointmentResponseDto(
            appointment.getId(),
            appointment.getDoctor().getId(),
            appointment.getDoctor().getName(),
            appointment.getDoctor().getSpecialization(),
            appointment.getPatient().getId(),
            appointment.getPatient().getFullName(),
            appointment.getAppointmentTime(),
            appointment.getEndTime(),
            appointment.getStatus(),
            appointment.getReason(),
            appointment.getCreatedAt()
        );
    }
}
