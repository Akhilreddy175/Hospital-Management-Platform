-- V2__seed_synthetic_data.sql
-- Synthetic Seed Data (Strictly synthetic, no real patient data)

-- Passwords are all BCrypt-hashed 'password123'
-- Hash: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
INSERT INTO users (email, password_hash, full_name, role)
VALUES 
    ('admin@hospital.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'System Administrator', 'ROLE_ADMIN'),
    ('patient@hospital.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'John Doe (Synthetic Patient)', 'ROLE_PATIENT'),
    ('patient2@hospital.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Jane Smith (Synthetic Patient)', 'ROLE_PATIENT')
ON CONFLICT (email) DO NOTHING;

INSERT INTO doctors (name, specialization, consultation_fee, is_active)
VALUES 
    ('Dr. Sarah Johnson', 'Cardiology', 150.00, true),
    ('Dr. Michael Chen', 'Neurology', 180.00, true),
    ('Dr. Priya Sharma', 'Pediatrics', 120.00, true),
    ('Dr. David Miller', 'Orthopedics', 140.00, true)
ON CONFLICT DO NOTHING;
