package com.algions.hospital.repository;

import com.algions.hospital.entity.Appointment;
import com.algions.hospital.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a 
        WHERE a.doctor.id = :doctorId 
          AND a.appointmentTime = :slotTime 
          AND a.status != 'CANCELLED'
    """)
    boolean existsActiveSlot(
        @Param("doctorId") Long doctorId,
        @Param("slotTime") OffsetDateTime slotTime
    );

    List<Appointment> findByPatientIdOrderByAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
        Long doctorId,
        OffsetDateTime start,
        OffsetDateTime end
    );
}
