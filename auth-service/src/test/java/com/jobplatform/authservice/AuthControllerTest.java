package com.jobplatform.authservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldReturnHealthStatus() throws Exception {
    mockMvc.perform(get("/api/auth/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("healthy"));
  }

  @Test
  void loginWithCorrectCredentialsShouldReturnForcePasswordChange() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"cosmin\",\"password\":\"minimouse\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("cosmin"))
        .andExpect(jsonPath("$.forcePasswordChange").value(true));
  }

  @Test
  void loginWithWrongPasswordShouldReturn401() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"cosmin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changePasswordShouldClearForceFlag() throws Exception {
    mockMvc.perform(post("/api/auth/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"cosmin\",\"currentPassword\":\"minimouse\",\"newPassword\":\"newpass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password changed successfully"));

    // după schimbare, forcePasswordChange trebuie să fie false
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"cosmin\",\"password\":\"newpass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.forcePasswordChange").value(false));
  }
}
