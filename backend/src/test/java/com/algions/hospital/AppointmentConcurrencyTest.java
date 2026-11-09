package com.algions.hospital;

import com.algions.hospital.dto.AppointmentRequestDto;
import com.algions.hospital.dto.AppointmentResponseDto;
import com.algions.hospital.entity.Doctor;
import com.algions.hospital.entity.User;
import com.algions.hospital.exception.SlotConflictException;
import com.algions.hospital.repository.AppointmentRepository;
import com.algions.hospital.repository.DoctorRepository;
import com.algions.hospital.repository.UserRepository;
import com.algions.hospital.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AppointmentConcurrencyTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    private Doctor testDoctor;
    private User patient1;
    private User patient2;

    @BeforeEach
    void setup() {
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        userRepository.deleteAll();

        testDoctor = doctorRepository.save(new Doctor(null, "Dr. Concurrency Specialist", "General Medicine", BigDecimal.valueOf(100.00), true));
        patient1 = userRepository.save(new User(null, "patient1@test.com", "hash", "Patient One", "ROLE_PATIENT"));
        patient2 = userRepository.save(new User(null, "patient2@test.com", "hash", "Patient Two", "ROLE_PATIENT"));
    }

    @Test
    @DisplayName("Acceptance Test: Two concurrent requests for the same doctor and slot MUST NOT both succeed")
    void testConcurrentBookingConflict() throws InterruptedException {
        OffsetDateTime targetSlot = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);

        AppointmentRequestDto request = new AppointmentRequestDto(
            testDoctor.getId(),
            targetSlot,
            "Routine checkup"
        );

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // Task for Patient 1
        executor.submit(() -> {
            try {
                startGate.await(); // Wait for simultaneous trigger
                AppointmentResponseDto res = appointmentService.bookAppointment(request, patient1.getEmail());
                if (res != null) {
                    successCount.incrementAndGet();
                }
            } catch (SlotConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                // Any other exception
            } finally {
                endGate.countDown();
            }
        });

        // Task for Patient 2
        executor.submit(() -> {
            try {
                startGate.await(); // Wait for simultaneous trigger
                AppointmentResponseDto res = appointmentService.bookAppointment(request, patient2.getEmail());
                if (res != null) {
                    successCount.incrementAndGet();
                }
            } catch (SlotConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                // Any other exception
            } finally {
                endGate.countDown();
            }
        });

        // Release both threads simultaneously
        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);

        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(1, successCount.get(), "Exactly ONE concurrent booking must succeed");
        assertEquals(1, conflictCount.get(), "Exactly ONE concurrent booking must be rejected with a slot conflict");
        assertEquals(1, appointmentRepository.count(), "Database must store strictly 1 confirmed appointment for the slot");
    }
}
