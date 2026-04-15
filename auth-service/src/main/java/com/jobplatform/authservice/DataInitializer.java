package com.jobplatform.authservice;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public DataInitializer(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.findByUsername("cosmin").isEmpty()) {
      User user = new User();
      user.setUsername("cosmin");
      user.setPasswordHash(passwordEncoder.encode("minimouse"));
      user.setForcePasswordChange(true);
      userRepository.save(user);
      System.out.println("Default user 'cosmin' created.");
    }
  }
}
