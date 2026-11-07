package com.jobplatform.authservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final String defaultUsername;
  private final String defaultPassword;

  public DataInitializer(
      UserRepository userRepository,
      BCryptPasswordEncoder passwordEncoder,
      @Value("${auth.default-username:cosmin}") String defaultUsername,
      @Value("${auth.default-password:}") String defaultPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.defaultUsername = defaultUsername;
    this.defaultPassword = defaultPassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (defaultPassword.isBlank()) {
      System.out.println("AUTH_DEFAULT_PASSWORD not set; skipping default user creation.");
      return;
    }
    if (userRepository.findByUsername(defaultUsername).isEmpty()) {
      User user = new User();
      user.setUsername(defaultUsername);
      user.setPasswordHash(passwordEncoder.encode(defaultPassword));
      user.setForcePasswordChange(true);
      userRepository.save(user);
      System.out.println("Default user '" + defaultUsername + "' created.");
    }
  }
}
