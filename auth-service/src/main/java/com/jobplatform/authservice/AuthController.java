package com.jobplatform.authservice;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public AuthController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping("/status")
  public Map<String, String> status() {
    return Map.of("issuer", "job-platform", "status", "healthy");
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    return userRepository.findByUsername(req.username())
        .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
        .map(u -> ResponseEntity.ok((Object) Map.of(
            "username", u.getUsername(),
            "role", u.getRole(),
            "forcePasswordChange", u.isForcePasswordChange()
        )))
        .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid username or password")));
  }

  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req) {
    return userRepository.findByUsername(req.username())
        .filter(u -> passwordEncoder.matches(req.currentPassword(), u.getPasswordHash()))
        .map(u -> {
          u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
          u.setForcePasswordChange(false);
          userRepository.save(u);
          return ResponseEntity.ok((Object) Map.of("message", "Password changed successfully"));
        })
        .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid username or current password")));
  }
}
