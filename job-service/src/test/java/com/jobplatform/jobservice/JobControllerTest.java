package com.jobplatform.jobservice;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobController.class)
class JobControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JobProviderService jobProviderService;

  @Test
  void shouldReturnAllJobs() throws Exception {
    when(jobProviderService.listJobs(null, null, null)).thenReturn(List.of(
        new JobListing("1", "Senior Platform Engineer", "CloudScale Labs", "Remote", "Full-time", true, List.of("Kubernetes"), "https://example.com/1", "Fallback", "2026-04-01"),
        new JobListing("2", "Backend Engineer", "Nimbus Systems", "Berlin", "Hybrid", false, List.of("Java"), "https://example.com/2", "Fallback", "2026-03-31")
    ));

    mockMvc.perform(get("/api/jobs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Senior Platform Engineer"))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void shouldForwardFilters() throws Exception {
    when(jobProviderService.listJobs(eq("backend"), eq("berlin"), eq(true))).thenReturn(List.of(
        new JobListing("3", "Staff Backend Engineer", "Acme", "Berlin", "Full-time", true, List.of("Java"), "https://example.com/3", "Arbeitnow", "2026-03-30")
    ));

    mockMvc.perform(get("/api/jobs").param("q", "backend").param("location", "berlin").param("remote", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Staff Backend Engineer"))
        .andExpect(jsonPath("$.length()").value(1));
    verify(jobProviderService).listJobs("backend", "berlin", true);
  }
}
