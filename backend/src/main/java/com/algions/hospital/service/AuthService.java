package com.algions.hospital.service;

import com.algions.hospital.dto.AuthRequestDto;
import com.algions.hospital.dto.AuthResponseDto;
import com.algions.hospital.entity.User;
import com.algions.hospital.exception.ResourceNotFoundException;
import com.algions.hospital.repository.UserRepository;
import com.algions.hospital.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        JwtTokenProvider tokenProvider,
        AuditService auditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    public AuthResponseDto login(AuthRequestDto request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.email()));

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole());
        auditService.recordEvent("USER_LOGIN", "USER", user.getId(), user.getEmail(), "Successful authentication");

        return new AuthResponseDto(token, user.getEmail(), user.getRole(), tokenProvider.getExpirationMs());
    }
}
