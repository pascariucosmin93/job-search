package com.jobplatform.authservice;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @GetMapping("/status")
  public Map<String, String> status() {
    return Map.of("issuer", "job-platform", "status", "healthy");
  }
}

