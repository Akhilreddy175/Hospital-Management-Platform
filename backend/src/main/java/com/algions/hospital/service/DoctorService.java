package com.algions.hospital.service;

import com.algions.hospital.dto.DoctorResponseDto;
import com.algions.hospital.entity.Doctor;
import com.algions.hospital.exception.ResourceNotFoundException;
import com.algions.hospital.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponseDto> getAllActiveDoctors() {
        return doctorRepository.findByIsActiveTrue().stream()
            .map(DoctorResponseDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
    }

    public List<OffsetDateTime> getAvailableSlots(Long doctorId, LocalDate date) {
        // Generates standard hourly slots from 09:00 to 17:00 UTC
        getDoctorById(doctorId); // Validate doctor existence
        List<OffsetDateTime> slots = new ArrayList<>();
        for (int hour = 9; hour < 17; hour++) {
            slots.add(date.atTime(hour, 0).atOffset(ZoneOffset.UTC));
        }
        return slots;
    }
}
