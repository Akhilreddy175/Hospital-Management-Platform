package com.algions.hospital.service;

import com.algions.hospital.dto.AppointmentRequestDto;
import com.algions.hospital.dto.AppointmentResponseDto;
import com.algions.hospital.dto.RescheduleRequestDto;
import com.algions.hospital.entity.Appointment;
import com.algions.hospital.entity.AppointmentStatus;
import com.algions.hospital.entity.Doctor;
import com.algions.hospital.entity.User;
import com.algions.hospital.exception.ResourceNotFoundException;
import com.algions.hospital.exception.SlotConflictException;
import com.algions.hospital.repository.AppointmentRepository;
import com.algions.hospital.repository.DoctorRepository;
import com.algions.hospital.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AppointmentService(
        AppointmentRepository appointmentRepository,
        DoctorRepository doctorRepository,
        UserRepository userRepository,
        AuditService auditService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AppointmentResponseDto bookAppointment(AppointmentRequestDto request, String patientEmail) {
        User patient = userRepository.findByEmail(patientEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientEmail));

        Doctor doctor = doctorRepository.findById(request.doctorId())
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.doctorId()));

        if (!doctor.isActive()) {
            throw new IllegalArgumentException("Doctor is currently inactive for consultations");
        }

        OffsetDateTime startTime = request.appointmentTime();
        OffsetDateTime endTime = startTime.plusMinutes(45); // 45-minute consultation window

        // Application-level conflict check
        if (appointmentRepository.existsActiveSlot(doctor.getId(), startTime)) {
            throw new SlotConflictException(
                "Doctor " + doctor.getName() + " already has a booked appointment at " + startTime
            );
        }

        Appointment appointment = new Appointment(doctor, patient, startTime, endTime, request.reason());

        try {
            Appointment saved = appointmentRepository.saveAndFlush(appointment);
            auditService.recordEvent(
                "APPOINTMENT_BOOKED",
                "APPOINTMENT",
                saved.getId(),
                patientEmail,
                "Booked slot with " + doctor.getName() + " at " + startTime
            );
            return AppointmentResponseDto.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            // Catches database-level unique constraint uk_doctor_time_slot in concurrent conditions
            throw new SlotConflictException(
                "Slot collision: The requested slot at " + startTime + " has just been booked by another request."
            );
        }
    }

    @Transactional
    public AppointmentResponseDto rescheduleAppointment(Long appointmentId, RescheduleRequestDto request, String patientEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getPatient().getEmail().equals(patientEmail)) {
            throw new IllegalArgumentException("You can only reschedule your own appointments");
        }

        OffsetDateTime newTime = request.newAppointmentTime();
        if (appointmentRepository.existsActiveSlot(appointment.getDoctor().getId(), newTime)) {
            throw new SlotConflictException("The newly requested slot is already booked for this doctor");
        }

        appointment.setAppointmentTime(newTime);
        appointment.setEndTime(newTime.plusMinutes(45));
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        Appointment updated = appointmentRepository.save(appointment);
        auditService.recordEvent(
            "APPOINTMENT_RESCHEDULED",
            "APPOINTMENT",
            updated.getId(),
            patientEmail,
            "Rescheduled to " + newTime
        );
        return AppointmentResponseDto.fromEntity(updated);
    }

    @Transactional
    public void cancelAppointment(Long appointmentId, String patientEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));

        if (!appointment.getPatient().getEmail().equals(patientEmail)) {
            throw new IllegalArgumentException("You can only cancel your own appointments");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        auditService.recordEvent(
            "APPOINTMENT_CANCELLED",
            "APPOINTMENT",
            appointment.getId(),
            patientEmail,
            "Cancelled appointment ID " + appointmentId
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getPatientAppointments(String patientEmail) {
        User patient = userRepository.findByEmail(patientEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientEmail));

        return appointmentRepository.findByPatientIdOrderByAppointmentTimeDesc(patient.getId()).stream()
            .map(AppointmentResponseDto::fromEntity)
            .toList();
    }
}
