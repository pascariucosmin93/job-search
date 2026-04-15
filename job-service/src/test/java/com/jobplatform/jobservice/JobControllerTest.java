package com.jobplatform.jobservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldReturnAllJobs() throws Exception {
    mockMvc.perform(get("/api/jobs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Senior Platform Engineer"))
        .andExpect(jsonPath("$.length()").value(7));
  }

  @Test
  void shouldSearchByTitle() throws Exception {
    mockMvc.perform(get("/api/jobs?q=backend"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Staff Backend Developer"))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void shouldSearchByLocation() throws Exception {
    mockMvc.perform(get("/api/jobs?q=remote"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
  }

  @Test
  void shouldSearchByTag() throws Exception {
    mockMvc.perform(get("/api/jobs?q=kubernetes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
  }
}
