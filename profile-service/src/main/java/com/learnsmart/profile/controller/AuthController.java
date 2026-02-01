package com.learnsmart.profile.controller;

import com.learnsmart.profile.dto.ProfileDtos.UserRegistrationRequest;
import com.learnsmart.profile.dto.ProfileDtos.UserProfileResponse;
import com.learnsmart.profile.dto.ProfileDtos.LoginRequest;
import com.learnsmart.profile.dto.ProfileDtos.LoginResponse;
import com.learnsmart.profile.service.ProfileServiceImpl;
import com.learnsmart.profile.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ProfileServiceImpl profileService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestBody @Valid UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        // For now, we'll implement a simple login that validates against existing users
        // In production, you'd validate password hash
        UserProfileResponse profile = profileService.findByEmail(request.getEmail());
        
        // TODO: Implement proper password validation
        // For now, we'll accept any email that exists
        
        String token = jwtUtil.generateToken(profile.getEmail(), profile.getUserId());
        
        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(profile.getUserId())
                .email(profile.getEmail())
                .displayName(profile.getDisplayName())
                .build());
    }
}
