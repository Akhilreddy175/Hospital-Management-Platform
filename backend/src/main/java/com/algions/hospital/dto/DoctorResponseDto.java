package com.algions.hospital.dto;

import com.algions.hospital.entity.Doctor;
import java.math.BigDecimal;

public record DoctorResponseDto(
    Long id,
    String name,
    String specialization,
    BigDecimal consultationFee,
    boolean isActive
) {
    public static DoctorResponseDto fromEntity(Doctor doctor) {
        return new DoctorResponseDto(
            doctor.getId(),
            doctor.getName(),
            doctor.getSpecialization(),
            doctor.getConsultationFee(),
            doctor.isActive()
        );
    }
}
