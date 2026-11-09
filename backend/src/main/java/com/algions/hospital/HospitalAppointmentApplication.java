package com.algions.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HospitalAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalAppointmentApplication.class, args);
    }
}
