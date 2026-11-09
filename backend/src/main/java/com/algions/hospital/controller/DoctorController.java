package com.algions.hospital.controller;

import com.algions.hospital.dto.DoctorResponseDto;
import com.algions.hospital.service.DoctorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponseDto>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllActiveDoctors());
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<OffsetDateTime>> getSlots(
        @PathVariable Long id,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now().plusDays(1);
        return ResponseEntity.ok(doctorService.getAvailableSlots(id, queryDate));
    }
}
