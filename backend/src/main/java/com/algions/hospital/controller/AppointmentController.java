package com.algions.hospital.controller;

import com.algions.hospital.dto.AppointmentRequestDto;
import com.algions.hospital.dto.AppointmentResponseDto;
import com.algions.hospital.dto.RescheduleRequestDto;
import com.algions.hospital.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> bookAppointment(
        @Valid @RequestBody AppointmentRequestDto request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        AppointmentResponseDto response = appointmentService.bookAppointment(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDto> rescheduleAppointment(
        @PathVariable Long id,
        @Valid @RequestBody RescheduleRequestDto request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        appointmentService.cancelAppointment(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponseDto>> getMyAppointments(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(userDetails.getUsername()));
    }
}
